package abstractdebugging;

import api.GoblintService;
import api.messages.GoblintARGLookupResult;
import api.messages.GoblintLocation;
import api.messages.GoblintPosition;
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
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractDebuggingServer implements IDebugProtocolServer {

    private static final int CFG_STEP_OFFSET = 1_000_000;
    private static final int ENTRY_STEP_OFFSET = 2_000_000;

    private final MagpieServer magpieServer;
    private final GoblintService goblintService;

    private IDebugProtocolClient client;
    private CompletableFuture<Void> configurationDoneFuture = new CompletableFuture<>();

    private final AtomicInteger nextThreadId = new AtomicInteger(0);

    private final List<GoblintLocation> breakpoints = new ArrayList<>();
    private int activeBreakpoint = -1;
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
        capabilities.setSupportsStepInTargetsRequest(true);
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        // TODO: Handle cases where Goblint expected path is not relative to current working directory
        String sourcePath = Path.of(System.getProperty("user.dir")).relativize(Path.of(args.getSource().getPath())).toString();
        log.info("Setting breakpoints for " + args.getSource().getPath() + " (" + sourcePath + ")");

        List<GoblintLocation> newBreakpoints = Arrays.stream(args.getBreakpoints())
                .map(breakpoint -> new GoblintLocation(
                        sourcePath,
                        breakpoint.getLine(),
                        breakpoint.getColumn() == null ? 0 : breakpoint.getColumn()
                ))
                .toList();

        breakpoints.removeIf(b -> b.getFile().equals(sourcePath));
        breakpoints.addAll(newBreakpoints);

        var response = new SetBreakpointsResponse();
        var setBreakpoints = newBreakpoints.stream()
                .map(location -> {
                    var breakpoint = new Breakpoint();
                    breakpoint.setLine(location.getLine());
                    breakpoint.setColumn(location.getColumn());
                    breakpoint.setSource(args.getSource());
                    breakpoint.setVerified(true);
                    return breakpoint;
                })
                .toArray(Breakpoint[]::new);
        response.setBreakpoints(setBreakpoints);
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
                    activeBreakpoint = -1;
                    runToNextBreakpoint();
                });
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        runToNextBreakpoint();
        return CompletableFuture.completedFuture(new ContinueResponse());
    }

    // TODO: Figure out if entry and return nodes contain any meaningful info and if not then skip them in all step methods

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        var targetThread = threads.get(args.getThreadId());
        if (targetThread.currentNode.outgoingCFGEdges.isEmpty()) {
            var stepOutArgs = new StepOutArguments();
            stepOutArgs.setThreadId(args.getThreadId());
            stepOutArgs.setSingleThread(args.getSingleThread());
            stepOutArgs.setGranularity(args.getGranularity());
            return stepOut(stepOutArgs);
        }
        if (targetThread.currentNode.outgoingCFGEdges.size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Branching control flow. Use step into target to choose the desired branch."));
        }
        var targetEdge = targetThread.currentNode.outgoingCFGEdges.get(0);
        stepAllThreadsToCFGNode(args.getThreadId(), targetEdge.cfgNodeId, n -> n.outgoingCFGEdges);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        var targetThread = threads.get(args.getThreadId());

        int targetId;
        if (args.getTargetId() != null) {
            targetId = args.getTargetId();
        } else if (targetThread.currentNode.outgoingEntryEdges.size() == 1) {
            targetId = ENTRY_STEP_OFFSET;
        } else if (targetThread.currentNode.outgoingEntryEdges.size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Ambiguous function call. Use step into target to choose the desired call"));
        } else {
            var nextArgs = new NextArguments();
            nextArgs.setThreadId(args.getThreadId());
            nextArgs.setSingleThread(args.getSingleThread());
            nextArgs.setGranularity(args.getGranularity());
            return next(nextArgs);
        }

        if (targetId >= ENTRY_STEP_OFFSET) {
            int targetIndex = targetId - ENTRY_STEP_OFFSET;
            var targetEdge = targetThread.currentNode.outgoingEntryEdges.get(targetIndex);
            stepAllThreadsToCFGNode(args.getThreadId(), targetEdge.cfgNodeId, n -> n.outgoingEntryEdges);
            return CompletableFuture.completedFuture(null);
        } else if (targetId >= CFG_STEP_OFFSET) {
            int targetIndex = targetId - CFG_STEP_OFFSET;
            var targetEdge = targetThread.currentNode.outgoingCFGEdges.get(targetIndex);
            stepAllThreadsToCFGNode(args.getThreadId(), targetEdge.cfgNodeId, n -> n.outgoingCFGEdges);
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown step in target: " + targetId));
        }
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        var thread = threads.get(args.getFrameId());

        List<StepInTarget> targets = new ArrayList<>();
        if (thread.currentNode != null) {
            var entryEdges = thread.currentNode.outgoingEntryEdges;
            for (int i = 0; i < entryEdges.size(); i++) {
                var edge = entryEdges.get(i);

                var target = new StepInTarget();
                target.setId(ENTRY_STEP_OFFSET + i);
                target.setLabel("call: " + edge.function + "(" + String.join(", ", edge.args) + ")");
                target.setLine(thread.currentNode.location.getLine());
                target.setColumn(thread.currentNode.location.getColumn());
                target.setEndLine(thread.currentNode.location.getEndLine());
                target.setEndColumn(thread.currentNode.location.getEndColumn());
                targets.add(target);
            }

            // Only show CFG edges as step in targets if there is branching
            if (thread.currentNode.outgoingCFGEdges.size() > 1) {
                var cfgEdges = thread.currentNode.outgoingCFGEdges;
                for (int i = 0; i < cfgEdges.size(); i++) {
                    var edge = cfgEdges.get(i);
                    var node = lookupNode(edge.nodeId);

                    var target = new StepInTarget();
                    target.setId(CFG_STEP_OFFSET + i);
                    target.setLabel("branch: " + edge.statementDisplayString);
                    target.setLine(node.location.getLine());
                    target.setColumn(node.location.getColumn());
                    target.setEndLine(node.location.getEndLine());
                    target.setEndColumn(node.location.getEndColumn());
                    targets.add(target);
                }
            }

            // Sort targets by the order they appear in code
            targets.sort(Comparator.comparing(StepInTarget::getLine).thenComparing(StepInTarget::getColumn));
        }

        var response = new StepInTargetsResponse();
        response.setTargets(targets.toArray(StepInTarget[]::new));
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        // TODO
        return CompletableFuture.failedFuture(userFacingError("Unsupported operation: Step out not implemented"));
    }

    private void runToNextBreakpoint() {
        // Note: We treat breaking on entry as the only breakpoint if no breakpoints are set.
        while (activeBreakpoint + 1 < Math.max(1, breakpoints.size())) {
            activeBreakpoint += 1;

            String stopReason;
            GoblintLocation targetLocation;
            List<NodeInfo> targetNodes;
            if (breakpoints.size() == 0) {
                stopReason = "entry";
                targetLocation = null;
                targetNodes = lookupNodes(new LookupParams());
            } else {
                stopReason = "breakpoint";
                targetLocation = breakpoints.get(activeBreakpoint);
                targetNodes = lookupNodes(new LookupParams(targetLocation)).stream()
                        .filter(n -> n.location.getLine() <= targetLocation.getLine() && targetLocation.getLine() <= n.location.getEndLine())
                        .toList();
                if (!targetNodes.isEmpty()) {
                    String cfgNodeId = targetNodes.get(0).cfgNodeId; // TODO: Is picking the first CFG node a good way to choose which nodes to keep?
                    targetNodes = targetNodes.stream().filter(n -> n.cfgNodeId.equals(cfgNodeId)).toList();
                }
            }

            if (!targetNodes.isEmpty()) {
                threads = new LinkedHashMap<>();
                for (var node : targetNodes) {
                    var state = new ThreadState();
                    state.name = "(" + node.contextId + ")[" + node.pathId + "]"; // TODO: Add function identifier
                    state.currentNode = node;
                    threads.put(newThreadId(), state);
                }

                var event = new StoppedEventArguments();
                event.setReason(stopReason);
                event.setThreadId(threads.keySet().stream().findFirst().orElse(null));
                event.setAllThreadsStopped(true);
                client.stopped(event);

                log.info("Stopped on breakpoint " + activeBreakpoint + " (" + targetLocation + ")");
                return;
            }

            // TODO: Should somehow notify the client that the breakpoint is unreachable?
            log.info("Skipped unreachable breakpoint " + activeBreakpoint + " (" + targetLocation + ")");
        }

        log.info("All breakpoints visited. Terminating debugger.");
        var event = new TerminatedEventArguments();
        client.terminated(event);
    }

    private void stepAllThreadsToCFGNode(int primaryThreadId, String targetCFGNodeId, Function<NodeInfo, List<? extends EdgeInfo>> candidateEdges) {
        for (var thread : threads.values()) {
            if (thread.currentNode == null) {
                // TODO: Try to recover thread if branches join
                continue;
            }
            String nextNodeId = candidateEdges.apply(thread.currentNode).stream()
                    .filter(e -> e.cfgNodeId.equals(targetCFGNodeId))
                    .map(e -> e.nodeId)
                    .findAny().orElse(null);
            thread.currentNode = nextNodeId == null ? null : lookupNode(nextNodeId);
        }

        // Sending the stopped event before the response to the step request is a violation of the DAP spec.
        // There is no clean way to do the operations in the correct order with lsp4j (see https://github.com/eclipse/lsp4j/issues/229),
        // multiple debug adapters seem to have the same issue, including the official https://github.com/microsoft/vscode-mock-debug,
        // and this has caused no issues in testing with VSCode.
        // Given all these considerations doing this in the wrong order is considered acceptable for now.
        // TODO: If https://github.com/eclipse/lsp4j/issues/229 ever gets resolved do this in the correct order.
        var event = new StoppedEventArguments();
        event.setReason("step");
        event.setThreadId(primaryThreadId);
        event.setAllThreadsStopped(true);
        client.stopped(event);
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
        if (thread.currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError("Unreachable"));
        }
        // TODO: Track and return multiple stack frames
        var stackFrame = new StackFrame();
        var location = thread.currentNode.location;
        stackFrame.setId(args.getThreadId());
        stackFrame.setName(thread.currentNode.nodeId); // TODO: Add function identifier
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
        // TODO: Blocked by Goblint server not providing access to ARG node state
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

    private String getAbsolutePath(String path) {
        return new File(path).getAbsolutePath();
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
                .thenApply(result -> result.stream()
                        .map(lookupResult -> {
                            NodeInfo nodeInfo = lookupResult.toNodeInfo();
                            if (!nodeInfo.outgoingReturnEdges.isEmpty() && nodeInfo.outgoingCFGEdges.isEmpty()) {
                                // Location of return nodes is generally the entire function.
                                // That looks strange, so we patch it to be only the end of the last line of the function.
                                // TODO: Maybe it would be better to adjust location when returning stack so the node info retains the original location
                                return nodeInfo.withLocation(new GoblintLocation(
                                        nodeInfo.location.getFile(),
                                        nodeInfo.location.getEndLine(), nodeInfo.location.getEndColumn(),
                                        nodeInfo.location.getEndLine(), nodeInfo.location.getEndColumn()
                                ));
                            } else {
                                return nodeInfo;
                            }
                        })
                        .toList())
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
