package abstractdebugging;

import api.GoblintService;
import api.messages.ARGNodeParams;
import api.messages.GoblintLocation;
import api.messages.LookupParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AbstractDebuggingServer implements IDebugProtocolServer {

    private static final int CFG_STEP_OFFSET = 1_000_000;
    private static final int ENTRY_STEP_OFFSET = 2_000_000;

    /**
     * Multiplier for thread id in frame id.
     * Frame id is calculated as threadId * FRAME_ID_THREAD_ID_MULTIPLIER + frameIndex.
     */
    private static final int FRAME_ID_THREAD_ID_MULTIPLIER = 100_000;

    private final GoblintService goblintService;

    private IDebugProtocolClient client;
    private CompletableFuture<Void> configurationDoneFuture = new CompletableFuture<>();

    private final List<GoblintLocation> breakpoints = new ArrayList<>();
    private int activeBreakpoint = -1;
    private final AtomicInteger nextThreadId = new AtomicInteger();
    private final Map<Integer, ThreadState> threads = new LinkedHashMap<>();

    private final ExecutorService backgroundExecutorService = Executors.newSingleThreadExecutor(runnable -> {
        java.lang.Thread thread = new java.lang.Thread(runnable, "adb-background-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Logger log = LogManager.getLogger(AbstractDebuggingServer.class);


    public AbstractDebuggingServer(GoblintService goblintService) {
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
        capabilities.setSupportsStepBack(true);
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
                    runToNextBreakpoint(1);
                });
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        runToNextBreakpoint(1);
        return CompletableFuture.completedFuture(new ContinueResponse());
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        runToNextBreakpoint(-1);
        return CompletableFuture.completedFuture(null);
    }

    // TODO: Figure out if entry and return nodes contain any meaningful info and if not then skip them in all step methods

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        var targetThread = threads.get(args.getThreadId());
        var currentNode = targetThread.getCurrentFrame().getNode();
        if (currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step over. Location is unavailable."));
        } else if (currentNode.outgoingCFGEdges().isEmpty()) {
            if (currentNode.outgoingReturnEdges().isEmpty()) {
                return CompletableFuture.failedFuture(userFacingError("Cannot step over. Reached last statement."));
            }
            var stepOutArgs = new StepOutArguments();
            stepOutArgs.setThreadId(args.getThreadId());
            stepOutArgs.setSingleThread(args.getSingleThread());
            stepOutArgs.setGranularity(args.getGranularity());
            return stepOut(stepOutArgs);
        }
        for (var thread : threads.values()) {
            // TODO: It is assumed that this is sufficient to guarantee that the target node is unambiguously determined by the target CFG node for all threads.
            //  It would be better to validate this invariant directly. See comment in stepAllThreadsToCFGNode for list of possible approaches.
            NodeInfo node = thread.getCurrentFrame().getNode();
            if (node != null && node.outgoingCFGEdges().size() > 1 && !node.outgoingEntryEdges().isEmpty()) {
                return CompletableFuture.failedFuture(userFacingError("Ambiguous path through function" + (thread == targetThread ? "" : " for " + thread.getName()) +
                        ". Step into function to choose the desired path."));
            }
        }
        if (currentNode.outgoingCFGEdges().size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Branching control flow. Use step into target to choose the desired branch."));
        }
        var targetEdge = currentNode.outgoingCFGEdges().get(0);
        stepAllThreadsToCFGNode(targetEdge.cfgNodeId(), NodeInfo::outgoingCFGEdges, false);
        sendStepStopEvent(args.getThreadId());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        var currentNode = threads.get(args.getThreadId()).getCurrentFrame().getNode();
        if (currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step in. Location is unavailable."));
        }

        int targetId;
        if (args.getTargetId() != null) {
            targetId = args.getTargetId();
        } else if (currentNode.outgoingEntryEdges().size() == 1) {
            targetId = ENTRY_STEP_OFFSET;
        } else if (currentNode.outgoingEntryEdges().size() > 1) {
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
            var targetEdge = currentNode.outgoingEntryEdges().get(targetIndex);
            stepAllThreadsToCFGNode(targetEdge.cfgNodeId(), NodeInfo::outgoingEntryEdges, true);
        } else if (targetId >= CFG_STEP_OFFSET) {
            int targetIndex = targetId - CFG_STEP_OFFSET;
            var targetEdge = currentNode.outgoingCFGEdges().get(targetIndex);
            stepAllThreadsToCFGNode(targetEdge.cfgNodeId(), NodeInfo::outgoingCFGEdges, false);
        } else {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown step in target: " + targetId));
        }
        sendStepStopEvent(args.getThreadId());

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        int threadId = args.getFrameId() / FRAME_ID_THREAD_ID_MULTIPLIER;
        // int frameIndex = args.getFrameId() % FRAME_ID_THREAD_ID_MULTIPLIER;
        NodeInfo currentNode = threads.get(threadId).getCurrentFrame().getNode();

        List<StepInTarget> targets = new ArrayList<>();
        if (currentNode != null) {
            var entryEdges = currentNode.outgoingEntryEdges();
            for (int i = 0; i < entryEdges.size(); i++) {
                var edge = entryEdges.get(i);

                var target = new StepInTarget();
                target.setId(ENTRY_STEP_OFFSET + i);
                target.setLabel((edge.createsNewThread() ? "thread: " : "call: ") + edge.function() + "(" + String.join(", ", edge.args()) + ")");
                target.setLine(currentNode.location().getLine());
                target.setColumn(currentNode.location().getColumn());
                target.setEndLine(currentNode.location().getEndLine());
                target.setEndColumn(currentNode.location().getEndColumn());
                targets.add(target);
            }

            // Only show CFG edges as step in targets if there is no stepping over function calls and there is branching
            if (currentNode.outgoingEntryEdges().isEmpty() && currentNode.outgoingCFGEdges().size() > 1) {
                var cfgEdges = currentNode.outgoingCFGEdges();
                for (int i = 0; i < cfgEdges.size(); i++) {
                    var edge = cfgEdges.get(i);
                    var node = lookupNode(edge.nodeId());

                    var target = new StepInTarget();
                    target.setId(CFG_STEP_OFFSET + i);
                    target.setLabel("branch: " + edge.statementDisplayString());
                    target.setLine(node.location().getLine());
                    target.setColumn(node.location().getColumn());
                    target.setEndLine(node.location().getEndLine());
                    target.setEndColumn(node.location().getEndColumn());
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
        ThreadState targetThread = threads.get(args.getThreadId());
        if (targetThread.getCurrentFrame().getNode() == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Location is unavailable."));
        } else if (!targetThread.hasPreviousFrame()) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Reached top of call stack.")); // TODO: Improve wording
        } else if (targetThread.getPreviousFrame().isAmbiguousFrame()) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Call stack is ambiguous."));
        }

        NodeInfo targetCallNode = targetThread.getPreviousFrame().getNode();
        assert targetCallNode != null;
        if (targetCallNode.outgoingCFGEdges().isEmpty()) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Function never returns."));
        }

        Map<Integer, NodeInfo> targetNodes = new HashMap<>();
        for (var threadEntry : threads.entrySet()) {
            int threadId = threadEntry.getKey();
            ThreadState thread = threadEntry.getValue();

            // Skip all threads that have no known previous frame or whose previous frame has a different location compared to the target thread.
            if (!thread.hasPreviousFrame() || thread.getPreviousFrame().isAmbiguousFrame()
                    || (thread.getPreviousFrame().getNode() != null && !Objects.equals(thread.getPreviousFrame().getNode().cfgNodeId(), targetCallNode.cfgNodeId()))) {
                continue;
            }

            NodeInfo currentNode = thread.getCurrentFrame().getNode();

            NodeInfo targetNode;
            if (currentNode == null) {
                targetNode = null;
            } else {
                Set<String> returnNodeIds = findMatchingNodes(currentNode, NodeInfo::outgoingCFGEdges, e -> !e.outgoingReturnEdges().isEmpty()).stream()
                        .flatMap(n -> n.outgoingReturnEdges().stream())
                        .map(EdgeInfo::nodeId)
                        .collect(Collectors.toSet());

                NodeInfo currentCallNode = thread.getPreviousFrame().getNode();
                List<String> candidateTargetNodeIds = currentCallNode.outgoingCFGEdges().stream()
                        .map(EdgeInfo::nodeId)
                        .filter(returnNodeIds::contains)
                        .toList();

                if (candidateTargetNodeIds.isEmpty()) {
                    targetNode = null;
                } else if (candidateTargetNodeIds.size() == 1) {
                    targetNode = lookupNode(candidateTargetNodeIds.get(0));
                } else {
                    return CompletableFuture.failedFuture(userFacingError("Ambiguous return path" + (thread == targetThread ? "" : " for " + thread.getName()) +
                            ". Step to return manually to choose the desired path."));
                }
            }

            targetNodes.put(threadId, targetNode);
        }

        // Remove all threads that have no target node (note that threads with an unavailable (null) target node are kept).
        threads.keySet().removeIf(k -> !targetNodes.containsKey(k));
        // Remove topmost stack frame and step to target node
        for (var threadEntry : threads.entrySet()) {
            int threadId = threadEntry.getKey();
            ThreadState thread = threadEntry.getValue();

            thread.popFrame();
            thread.getCurrentFrame().setNode(targetNodes.get(threadId));
        }
        sendStepStopEvent(args.getThreadId());

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        var targetThread = threads.get(args.getThreadId());
        var currentNode = targetThread.getCurrentFrame().getNode();
        if (currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. Location is unavailable."));
        } else if (currentNode.incomingCFGEdges().isEmpty()) {
            // TODO: Support stepping back out of function if caller is unambiguous and has the same CFG location for all threads
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. Reached start of function."));
        } else if (currentNode.incomingCFGEdges().size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. Previous location is ambiguous."));
        }

        var targetCFGNodeId = currentNode.incomingCFGEdges().get(0).cfgNodeId();

        List<Pair<ThreadState, NodeInfo>> steps = new ArrayList<>();
        for (var thread : threads.values()) {
            var currentFrame = thread.getCurrentFrame();

            NodeInfo targetNode;
            if (currentFrame.getNode() != null) {
                List<CFGEdgeInfo> targetEdges = currentFrame.getNode().incomingCFGEdges().stream()
                        .filter(e -> e.cfgNodeId().equals(targetCFGNodeId))
                        .toList();
                if (targetEdges.isEmpty()) {
                    return CompletableFuture.failedFuture(userFacingError("Cannot step back. No matching path from " + thread.getName()));
                } else if (targetEdges.size() > 1) {
                    return CompletableFuture.failedFuture(userFacingError("Cannot step back. Path is ambiguous from " + thread.getName()));
                }
                targetNode = lookupNode(targetEdges.get(0).nodeId());
            } else if (currentFrame.getLastReachableNode() != null && currentFrame.getLastReachableNode().cfgNodeId().equals(targetCFGNodeId)) {
                targetNode = currentFrame.getLastReachableNode();
            } else {
                continue;
            }

            steps.add(Pair.of(thread, targetNode));
        }

        for (var step : steps) {
            ThreadState thread = step.getLeft();
            NodeInfo targetNode = step.getRight();
            thread.getCurrentFrame().setNode(targetNode);
        }

        sendStepStopEvent(args.getThreadId());

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        // TODO
        // TODO: Recover unreachable branches when jumping backwards to / over node where branches diverged.
        return IDebugProtocolServer.super.goto_(args);
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        // TODO
        return IDebugProtocolServer.super.gotoTargets(args);
    }

    /**
     * Runs to next breakpoint in given direction.
     *
     * @param direction 1 to run to next breakpoint, -1 to run to previous breakpoint.
     */
    private void runToNextBreakpoint(int direction) {
        // Note: We treat breaking on entry as the only breakpoint if no breakpoints are set.
        // TODO: Changing breakpoints when the debugger is active can cause breakpoints to be skipped or visited twice.
        while (activeBreakpoint + direction < Math.max(1, breakpoints.size()) && activeBreakpoint + direction >= 0) {
            activeBreakpoint += direction;

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
                        .filter(n -> n.location().getLine() <= targetLocation.getLine() && targetLocation.getLine() <= n.location().getEndLine())
                        .toList();
                if (!targetNodes.isEmpty()) {
                    // TODO: Instead we should get the first matching CFG node and then request corresponding ARG nodes for that.
                    String cfgNodeId = targetNodes.get(0).cfgNodeId();
                    targetNodes = targetNodes.stream().filter(n -> n.cfgNodeId().equals(cfgNodeId)).toList();
                }
            }

            if (!targetNodes.isEmpty()) {
                clearThreads();
                for (var node : targetNodes) {
                    var state = new ThreadState(
                            "breakpoint " + node.nodeId(),
                            assembleStackTrace(node)
                    );
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

    /**
     * Steps all threads to given CFG node.
     *
     * @throws IllegalFormatException if the target node is ambiguous ie there are multiple candidate edges that have the target CFG node.
     */
    private void stepAllThreadsToCFGNode(String targetCFGNodeId, Function<NodeInfo, List<? extends EdgeInfo>> candidateEdges, boolean addFrame) {
        // Note: It is important that all threads, including threads with unavailable location, are stepped, because otherwise the number of added stack frames could get out of sync.
        List<Pair<ThreadState, EdgeInfo>> steps = new ArrayList<>();
        for (var thread : threads.values()) {
            EdgeInfo targetEdge;
            if (thread.getCurrentFrame().getNode() == null) {
                targetEdge = null;
            } else {
                // This is unsound if there can be multiple distinct target edges with the same target CFG node.
                // This is possible when stepping over / out of function with internal path sensitive branching.
                // This may also be possible when stepping in results in multiple context of the same function.
                // TODO: Somehow ensure this can never happen.
                //  Options:
                //  * Throw error (current approach) (problem: might make it impossible to step at all in some cases. in some cases meaningful error messages and strict correctness are at odds)
                //  * Split thread into multiple threads. (problem: complicates 'step back' and maintaining thread ordering)
                //  * Identify true source of branching and use it to disambiguate (problem: there might not be a source of branching in all cases. complicates stepping logic)
                //  * Make ambiguous threads unavailable (problem: complicates mental model of when threads become unavailable. breaks 'step into targets' relying on this for stepping all threads)
                List<? extends EdgeInfo> targetEdges = candidateEdges.apply(thread.getCurrentFrame().getNode()).stream()
                        .filter(e -> e.cfgNodeId().equals(targetCFGNodeId))
                        .toList();
                if (targetEdges.size() > 1) {
                    throw new IllegalStateException("Invariant violated for " + thread.getName() + ". Expected 1 ARG node with CFG node " + targetCFGNodeId + " but found " + targetEdges.size() + ".");
                }
                targetEdge = targetEdges.size() == 1 ? targetEdges.get(0) : null;
            }

            steps.add(Pair.of(thread, targetEdge));
        }
        for (var step : steps) {
            ThreadState thread = step.getLeft();
            EdgeInfo targetEdge = step.getRight();
            NodeInfo targetNode = targetEdge == null ? null : lookupNode(targetEdge.nodeId());
            if (addFrame) {
                boolean isNewThread = targetEdge instanceof FunctionCallEdgeInfo fce && fce.createsNewThread();
                thread.pushFrame(new StackFrameState(targetNode, false, thread.getCurrentFrame().getLocalThreadIndex() - (isNewThread ? 1 : 0)));
            } else {
                thread.getCurrentFrame().setNode(targetNode);
            }
        }
    }

    private void sendStepStopEvent(int primaryThreadId) {
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
                    thread.setName(entry.getValue().getName());
                    return thread;
                })
                .toArray(Thread[]::new);
        response.setThreads(responseThreads);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        var thread = threads.get(args.getThreadId());
        if (thread.getCurrentFrame().getNode() == null) {
            return CompletableFuture.failedFuture(userFacingError("No matching path"));
        }

        final int currentThreadId = thread.getCurrentFrame().getLocalThreadIndex();
        StackFrame[] stackFrames = new StackFrame[thread.getFrames().size()];
        for (int i = 0; i < thread.getFrames().size(); i++) {
            var frame = thread.getFrames().get(i);
            assert frame.getNode() != null;

            var stackFrame = new StackFrame();
            stackFrame.setId(args.getThreadId() * FRAME_ID_THREAD_ID_MULTIPLIER + i);
            // TODO: Notation for ambiguous frames and parent threads could be clearer.
            stackFrame.setName((frame.isAmbiguousFrame() ? "? " : "") + (frame.getLocalThreadIndex() != currentThreadId ? "^" : "") + frame.getNode().function() + " " + frame.getNode().nodeId());
            var location = frame.getNode().location();
            stackFrame.setLine(location.getLine());
            stackFrame.setColumn(location.getColumn());
            stackFrame.setEndLine(location.getEndLine());
            stackFrame.setEndColumn(location.getEndColumn());
            var source = new Source();
            source.setName(location.getFile());
            source.setPath(new File(location.getFile()).getAbsolutePath());
            stackFrame.setSource(source);

            stackFrames[i] = stackFrame;
        }

        var response = new StackTraceResponse();
        response.setStackFrames(stackFrames);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        // TODO: Support multiple scopes

        var scope = new Scope();
        scope.setName("All");
        scope.setVariablesReference(args.getFrameId() + 1);

        var response = new ScopesResponse();
        response.setScopes(new Scope[]{scope});
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        // TODO: Support structured variables

        int threadId = (args.getVariablesReference() - 1) / FRAME_ID_THREAD_ID_MULTIPLIER;
        int frameIndex = (args.getVariablesReference() - 1) % FRAME_ID_THREAD_ID_MULTIPLIER;
        var frame = threads.get(threadId).getFrames().get(frameIndex);
        if (frame.getNode() == null) {
            throw new IllegalStateException("Attempt to request variables for unavailable frame " + threadId + "[" + frameIndex + "]");
        }

        var state = lookupState(frame.getNode().nodeId());

        var stateVariable = new Variable();
        stateVariable.setName("(arg/state)");
        stateVariable.setValue(state.toString());
        var lockedVariable = new Variable();
        lockedVariable.setName("<locked>");
        lockedVariable.setValue(domainValueToString(state.get("mutex")));
        var stateValues = state.get("base").getAsJsonObject().get("value domain").getAsJsonObject();
        var variables = Stream.concat(
                        Stream.of(stateVariable, lockedVariable),
                        stateValues.entrySet().stream()
                                // TODO: Temporary values should be shown when they are assigned to.
                                // TODO: If the user creates a variable named tmp then it will be hidden as well.
                                .filter(entry -> !entry.getKey().startsWith("tmp"))
                                .map(entry -> {
                                    var variable = new Variable();
                                    variable.setName(entry.getKey());
                                    variable.setValue(domainValueToString(entry.getValue()));
                                    //variable.setType("?");
                                    return variable;
                                })
                )
                .toArray(Variable[]::new);

        var response = new VariablesResponse();
        response.setVariables(variables);
        return CompletableFuture.completedFuture(response);
    }

    private String domainValueToString(JsonElement value) {
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        } else if (value.isJsonArray()) {
            return "{" + StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                    .map(this::domainValueToString)
                    .collect(Collectors.joining(", ")) + "}";
        } else if (value.isJsonObject()) {
            return "{" + value.getAsJsonObject().entrySet().stream()
                    .map(e -> e.getKey() + ": " + domainValueToString(e.getValue())) + "}";
        } else {
            throw new IllegalArgumentException("Unknown domain value type: " + value.getClass());
        }
    }

    // Helper methods:

    /**
     * Queues an action that should be executed after the response for the current request has been sent.
     * Note: The current implementation has a race condition and does not in fact guarantee that the action is executed after the response is sent.
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

    private void clearThreads() {
        nextThreadId.set(0);
        threads.clear();
    }

    private List<StackFrameState> assembleStackTrace(NodeInfo startNode) {
        int curThreadId = 0;
        List<StackFrameState> stackFrames = new ArrayList<>();
        stackFrames.add(new StackFrameState(startNode, false, curThreadId));
        NodeInfo entryNode;
        do {
            entryNode = getEntryNode(stackFrames.get(stackFrames.size() - 1).getNode());
            boolean ambiguous = entryNode.incomingEntryEdges().size() > 1;
            for (var edge : entryNode.incomingEntryEdges()) {
                if (edge.createsNewThread()) {
                    curThreadId += 1;
                }
                var node = lookupNode(edge.nodeId());
                stackFrames.add(new StackFrameState(node, ambiguous, curThreadId));
            }
        } while (entryNode.incomingEntryEdges().size() == 1);
        return stackFrames;
    }

    private NodeInfo getEntryNode(NodeInfo node) {
        NodeInfo entryNode = _getEntryNode(node, new HashSet<>());
        if (entryNode == null) {
            throw new IllegalStateException("Failed to find entry node for node " + node.nodeId());
        }
        return entryNode;
    }

    private NodeInfo _getEntryNode(NodeInfo node, Set<String> seenNodes) {
        if (node.incomingCFGEdges().isEmpty()) {
            return node;
        }
        if (seenNodes.contains(node.nodeId())) {
            return null;
        }
        seenNodes.add(node.nodeId());
        for (var edge : node.incomingCFGEdges()) {
            NodeInfo entryNode = _getEntryNode(lookupNode(edge.nodeId()), seenNodes);
            if (entryNode != null) {
                return entryNode;
            }
        }
        return null;
    }

    private List<NodeInfo> findMatchingNodes(NodeInfo node, Function<NodeInfo, Collection<? extends EdgeInfo>> candidateEdges, Predicate<NodeInfo> condition) {
        List<NodeInfo> foundNodes = new ArrayList<>();
        _findMatchingNodes(node, candidateEdges, condition, new HashSet<>(), foundNodes);
        return foundNodes;
    }

    private void _findMatchingNodes(NodeInfo node, Function<NodeInfo, Collection<? extends EdgeInfo>> candidateEdges, Predicate<NodeInfo> condition,
                                    Set<String> seenNodes, List<NodeInfo> foundNodes) {
        if (seenNodes.contains(node.nodeId())) {
            return;
        }
        seenNodes.add(node.nodeId());
        if (condition.test(node)) {
            foundNodes.add(node);
        }
        for (var edge : candidateEdges.apply(node)) {
            _findMatchingNodes(lookupNode(edge.nodeId()), candidateEdges, condition, seenNodes, foundNodes);
        }
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
                            if (!nodeInfo.outgoingReturnEdges().isEmpty() && nodeInfo.outgoingCFGEdges().isEmpty()) {
                                // Location of return nodes is generally the entire function.
                                // That looks strange, so we patch it to be only the end of the last line of the function.
                                // TODO: Maybe it would be better to adjust location when returning stack so the node info retains the original location
                                return nodeInfo.withLocation(new GoblintLocation(
                                        nodeInfo.location().getFile(),
                                        nodeInfo.location().getEndLine(), nodeInfo.location().getEndColumn(),
                                        nodeInfo.location().getEndLine(), nodeInfo.location().getEndColumn()
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
            case 0 -> throw userFacingError("Node with id " + nodeId + " not found");
            case 1 -> nodes.get(0);
            default -> throw userFacingError("Multiple nodes with id " + nodeId + " found");
        };
    }

    private JsonObject lookupState(String nodeId) {
        return goblintService.arg_state(new ARGNodeParams(nodeId)).join();
    }

}
