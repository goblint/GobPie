package abstractdebugging;

import api.GoblintService;
import api.messages.GoblintARGLookupResult;
import api.messages.GoblintLocation;
import api.messages.LookupParams;
import goblintserver.GoblintServer;
import magpiebridge.core.MagpieServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class AbstractDebuggingServer implements IDebugProtocolServer {

    private static final int CFG_STEP_OFFSET = 1_000_000;
    private static final int ENTRY_STEP_OFFSET = 2_000_000;

    private final MagpieServer magpieServer;
    private final GoblintService goblintService;

    private IDebugProtocolClient client;
    private CompletableFuture<Void> configurationDoneFuture = new CompletableFuture<>();

    private final AtomicInteger nextThreadId = new AtomicInteger(0);

    private List<GoblintLocation> breakpoints = List.of();
    private int activeBreakpoint;
    private Map<Integer, ThreadState> threads = Map.of();

    private final ExecutorService backgroundExecutorService = Executors.newSingleThreadExecutor(runnable -> {
        java.lang.Thread thread = new java.lang.Thread(runnable, "adb-background-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Logger log = LogManager.getLogger(AbstractDebuggingServer.class);


    public AbstractDebuggingServer(MagpieServer magpieServer, GoblintService goblintService) {
        this.magpieServer = magpieServer;
        this.goblintService = goblintService;
    }

    public void connectClient(IDebugProtocolClient client) {
        if (this.client != null) {
            throw new IllegalStateException("Client already connected");
        }
        this.client = client;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(true);
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        log.info("Setting breakpoints");
        // TODO
        var response = new SetBreakpointsResponse();
        response.setBreakpoints(new Breakpoint[0]);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        // TODO: This should not be called by the IDE given our reported capabilities, but VSCode calls it anyway. Why?
        var response = new SetExceptionBreakpointsResponse();
        response.setBreakpoints(new Breakpoint[0]);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        log.info("Debug adapter configuration done");
        configurationDoneFuture.complete(null);
        configurationDoneFuture = new CompletableFuture<>();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        // Attach doesn't make sense for abstract debugging, but to avoid issues in case the client requests it anyway we just treat it as a launch request.
        return launch(args);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        // Start configuration by notifying that client is initialized.
        client.initialized();
        log.info("Debug adapter initialized, waiting for configuration");
        // Wait for configuration to complete, then launch.
        return configurationDoneFuture
                .thenRun(() -> {
                    log.info("Debug adapter launched");
                    runToNextBreakpoint(true);
                });
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        runToNextBreakpoint(false);
        return CompletableFuture.completedFuture(new ContinueResponse());
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        var targetThread = threads.get(args.getThreadId());
        if (targetThread.currentNode.outgoingCFGEdges().isEmpty()) {
            var stepOutArgs = new StepOutArguments();
            stepOutArgs.setThreadId(args.getThreadId());
            stepOutArgs.setSingleThread(args.getSingleThread());
            stepOutArgs.setGranularity(args.getGranularity());
            return stepOut(stepOutArgs);
        }
        if (targetThread.currentNode.outgoingCFGEdges().size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step due to branching. Use step in to choose the desired branch."));
        }
        var targetEdge = targetThread.currentNode.outgoingCFGEdges().get(0);
        stepAllThreadsAlongEdge(args.getThreadId(), targetEdge, NodeInfo::outgoingCFGEdges);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        var targetThread = threads.get(args.getThreadId());
        if (args.getTargetId() >= ENTRY_STEP_OFFSET) {
            int targetIndex = args.getTargetId() - ENTRY_STEP_OFFSET;
            var targetEdge = targetThread.currentNode.outgoingEntryEdges().get(targetIndex);
            stepAllThreadsAlongEdge(args.getThreadId(), targetEdge, NodeInfo::outgoingEntryEdges);
        } else if (args.getTargetId() >= CFG_STEP_OFFSET) {
            int targetIndex = args.getTargetId() - CFG_STEP_OFFSET;
            var targetEdge = targetThread.currentNode.outgoingCFGEdges().get(targetIndex);
            stepAllThreadsAlongEdge(args.getThreadId(), targetEdge, NodeInfo::outgoingCFGEdges);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("Unknown step in target: " + args.getTargetId()));
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        // TODO
        return CompletableFuture.failedFuture(new NotSupportedException("Step out not implemented"));
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        var response = new StepInTargetsResponse();
        response.setTargets(new StepInTarget[0]);
        return CompletableFuture.completedFuture(response);
    }

    private void runToNextBreakpoint(boolean isLaunch) {
        // Note: We treat breaking on entry as the only breakpoint if no breakpoints are set.

        if (isLaunch) {
            activeBreakpoint = 0;
        } else {
            activeBreakpoint += 1;
            if (activeBreakpoint >= breakpoints.size()) {
                var event = new TerminatedEventArguments();
                client.terminated(event);
                return;
            }
        }

        List<NodeInfo> targetNodes;
        String stopReason;
        if (breakpoints.size() == 0) {
            targetNodes = lookupNodes(new LookupParams());
            stopReason = "entry";
        } else {
            targetNodes = lookupNodes(new LookupParams(breakpoints.get(activeBreakpoint)));
            stopReason = "breakpoint";
        }

        for (var threadEntry : threads.entrySet()) {
            var event = new ThreadEventArguments();
            event.setThreadId(threadEntry.getKey());
            event.setReason("exited");
            client.thread(event);
        }

        threads = new LinkedHashMap<>();
        for (var node : targetNodes) {
            var state = new ThreadState();
            state.name = node.nodeId(); // TODO: Use context info to construct name
            state.currentNode = node;
            threads.put(newThreadId(), state);
        }

        for (var threadEntry : threads.entrySet()) {
            var event = new ThreadEventArguments();
            event.setThreadId(threadEntry.getKey());
            event.setReason("started");
            client.thread(event);
        }

        var event = new StoppedEventArguments();
        event.setReason(stopReason);
        event.setThreadId(threads.keySet().stream().findFirst().orElse(null));
        //event.setAllThreadsStopped(true);
        client.stopped(event);

        log.info("Stopped on breakpoint " + activeBreakpoint);
    }

    private void stepAllThreadsAlongEdge(int primaryThreadId, EdgeInfo targetEdge, Function<NodeInfo, List<EdgeInfo>> candidateEdges) {
        for (var thread : threads.values()) {
            // TODO: Comparing edge parameters in this way is probably fairly inefficient.
            //  Also it might be unsound if some edges have the same fields in a different order in the parameters.
            String nextNodeId = candidateEdges.apply(thread.currentNode).stream()
                    .filter(e -> e.data().equals(targetEdge.data()))
                    .map(EdgeInfo::otherNodeId)
                    .findAny().orElse(null);
            thread.currentNode = nextNodeId == null ? null : lookupNode(nextNodeId);
        }

        // Sending the stopped event before the response to the step request is a violation of the DAP spec.
        // There is no clean way to do the operations in the correct order (see https://github.com/eclipse/lsp4j/issues/229),
        // multiple debug adapters seem to have the same issue, including the official https://github.com/microsoft/vscode-mock-debug,
        // and this has caused no issues in testing with VSCode.
        // Given all these considerations doing this in the wrong order is considered acceptable for now.
        // TODO: If ever gets resolved do this in the correct order.
        {
            // TODO: Why does stepping cause selected thread to lose focus in VSCode
            var event = new StoppedEventArguments();
            event.setReason("step");
            event.setThreadId(primaryThreadId);
            //event.setAllThreadsStopped(true);
            event.setPreserveFocusHint(true);
            client.stopped(event);
        }
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        var response = new ThreadsResponse();
        Thread[] responseThreads = threads.entrySet().stream()
                .map(entry -> {
                    Thread thread = new Thread();
                    thread.setId(entry.getKey());
                    thread.setName(entry.getValue().name);
                    return thread;
                })
                .toArray(Thread[]::new);
        response.setThreads(responseThreads);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        var thread = threads.get(args.getThreadId());
        var location = thread.currentNode.location();
        // TODO: Track and return multiple stack frames
        var stackFrame = new StackFrame();
        stackFrame.setId(args.getThreadId());
        stackFrame.setName(thread.name);
        stackFrame.setLine(location.getLine());
        stackFrame.setColumn(location.getColumn());
        stackFrame.setEndLine(location.getEndLine());
        stackFrame.setEndColumn(location.getEndColumn());
        var source = new Source();
        source.setName(location.getFile());
        source.setPath(new File(location.getFile()).getAbsolutePath());
        stackFrame.setSource(source);
        var response = new StackTraceResponse();
        response.setStackFrames(new StackFrame[]{stackFrame});
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        // TODO
        var response = new ScopesResponse();
        response.setScopes(new Scope[0]);
        return CompletableFuture.completedFuture(response);
    }

    // Helper methods:

    /**
     * Queues an action that should be executed after the response for the current request has been sent.
     * Note: The current implementation has a race condition and does not in fact guarantee that the action is executed after the response is sent.
     * <p>
     */
    private void queueAfterSendAction(Runnable action) {
        backgroundExecutorService.submit(() -> {
            java.lang.Thread.sleep(20);
            action.run();
            return null;
        });
    }

    private int newThreadId() {
        return nextThreadId.getAndIncrement();
    }

    /**
     * Returns an exception that will be shown in the IDE as the message with no modifications and no additional context.
     */
    private ResponseErrorException userFacingError(String message) {
        return new ResponseErrorException(new ResponseError(ResponseErrorCode.RequestFailed, message, null));
    }

    // Synchronous convenience methods around GoblintService:

    private List<NodeInfo> lookupNodes(LookupParams params) {
        return goblintService.arg_lookup(params)
                .thenApply(result -> result.stream().map(GoblintARGLookupResult::toNodeInfo).toList())
                .join();
    }

    private NodeInfo lookupNode(String nodeId) {
        var nodes = lookupNodes(new LookupParams(nodeId));
        return switch (nodes.size()) {
            case 0 -> null;
            case 1 -> nodes.get(0);
            default -> throw new RuntimeException("Node with id " + nodeId + " not found");
        };
    }

}
