package abstractdebugging;

import api.messages.GoblintLocation;
import api.messages.GoblintVarinfo;
import api.messages.params.LookupParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Abstract debugging server.
 * An instance of this corresponds to a single debugging session.
 * Implements the core logic of abstract debugging with the lsp4j DAP interface.
 *
 * @author Juhan Oskar Hennoste
 * @since 0.0.4
 */
public class AbstractDebuggingServer implements IDebugProtocolServer {

    private static final int TARGET_BLOCK_SIZE = 1_000_000;
    private static final int STEP_OVER_OFFSET = TARGET_BLOCK_SIZE;
    private static final int STEP_IN_OFFSET = 2 * TARGET_BLOCK_SIZE;
    private static final int STEP_BACK_OVER_OFFSET = 3 * TARGET_BLOCK_SIZE;
    private static final int STEP_BACK_OUT_OFFSET = 4 * TARGET_BLOCK_SIZE;

    /**
     * Multiplier for thread id in frame id.
     * Frame id is calculated as threadId * FRAME_ID_THREAD_ID_MULTIPLIER + frameIndex.
     */
    private static final int FRAME_ID_THREAD_ID_MULTIPLIER = 100_000;

    /**
     * Set of built-in and standard library variables. They are generally hidden in variable views to reduce noise.
     * List taken from <a href="https://github.com/goblint/analyzer/blob/master/src/framework/control.ml#L237-L243">is_std function in Goblint</a>.
     */
    private static final Set<String> STD_VARIABLES = Set.of(
            "__tzname", "__daylight", "__timezone", "tzname", "daylight", "timezone", // unix time.h
            "getdate_err", // unix time.h, but somehow always in MacOS even without include
            "stdin", "stdout", "stderr", // standard stdio.h
            "optarg", "optind", "opterr", "optopt", // unix unistd.h
            "__environ" // Linux Standard Base Core Specification
    );

    private final ResultsService resultsService;

    private final EventQueue eventQueue = new EventQueue();
    private IDebugProtocolClient client;
    private CompletableFuture<Void> configurationDoneFuture = new CompletableFuture<>();

    private final List<BreakpointInfo> breakpoints = new ArrayList<>();
    private int activeBreakpoint = -1;
    private final Map<Integer, ThreadState> threads = new LinkedHashMap<>();

    private final Map<String, Scope[]> nodeScopes = new HashMap<>();
    private final List<Variable[]> storedVariables = new ArrayList<>();

    private final Logger log = LogManager.getLogger(AbstractDebuggingServer.class);


    public AbstractDebuggingServer(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * Sets the client used to send events back to the debug adapter client (usually an IDE).
     */
    public void connectClient(IDebugProtocolClient client) {
        if (this.client != null) {
            throw new IllegalStateException("Client already connected");
        }
        this.client = client;
    }

    /**
     * Gets event queue where sending DAP events will be queued.
     */
    public EventQueue getEventQueue() {
        return eventQueue;
    }

    /**
     * DAP request to initialize debugger and report supported capabilities.
     * For the abstract debugger this is a no-op (except for returning supported capabilities).
     */
    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(true);
        capabilities.setSupportsStepInTargetsRequest(true);
        capabilities.setSupportsStepBack(true);
        capabilities.setSupportsConditionalBreakpoints(true);
        capabilities.setSupportsRestartFrame(true);
        capabilities.setSupportsTerminateThreadsRequest(true);
        return CompletableFuture.completedFuture(capabilities);
    }

    /**
     * DAP request to set breakpoints.
     */
    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        Path absoluteSourcePath = Path.of(args.getSource().getPath()).toAbsolutePath();
        String goblintSourcePath = resultsService.getGoblintTrackedFiles().stream()
                .filter(f -> Path.of(f).toAbsolutePath().equals(absoluteSourcePath))
                .findFirst().orElse(null);
        log.info("Setting breakpoints for " + args.getSource().getPath() + " (" + goblintSourcePath + ")");

        List<Breakpoint> newBreakpointStatuses = new ArrayList<>();
        List<BreakpointInfo> newBreakpoints = new ArrayList<>();
        for (var breakpoint : args.getBreakpoints()) {
            var breakpointStatus = new Breakpoint();
            newBreakpointStatuses.add(breakpointStatus);

            if (goblintSourcePath == null) {
                breakpointStatus.setVerified(false);
                breakpointStatus.setMessage("File not analyzed");
                continue;
            }

            var targetLocation = new GoblintLocation(goblintSourcePath, breakpoint.getLine(), breakpoint.getColumn() == null ? 0 : breakpoint.getColumn());
            CFGNodeInfo cfgNode;
            try {
                cfgNode = resultsService.lookupCFGNode(targetLocation);
            } catch (RequestFailedException e) {
                breakpointStatus.setVerified(false);
                breakpointStatus.setMessage("No statement found at location " + targetLocation);
                continue;
            }
            breakpointStatus.setSource(args.getSource());
            breakpointStatus.setLine(cfgNode.location().getLine());
            breakpointStatus.setColumn(cfgNode.location().getColumn());

            ConditionalExpression condition;
            if (breakpoint.getCondition() == null) {
                condition = null;
            } else {
                try {
                    condition = ConditionalExpression.fromString(breakpoint.getCondition(), true);
                } catch (IllegalArgumentException e) {
                    breakpointStatus.setVerified(false);
                    breakpointStatus.setMessage(e.getMessage());
                    continue;
                }
            }

            List<NodeInfo> targetNodes;
            try {
                targetNodes = findTargetNodes(cfgNode, condition);
            } catch (IllegalArgumentException e) {
                breakpointStatus.setVerified(false);
                // VSCode seems to use code formatting rules for conditional breakpoint messages.
                // The character ' causes VSCode to format any following text as a string, which looks strange and causes unwanted line breaks.
                // As a workaround all ' characters are replaced with a different Unicode apostrophe.
                // TODO: Find a way to fix this without manipulating the error message.
                //  Possibly this will need opening an issue in the VSCode issue tracker.
                breakpointStatus.setMessage(e.getMessage().replace('\'', '’'));
                continue;
            }

            newBreakpoints.add(new BreakpointInfo(cfgNode, condition, targetNodes));
            if (targetNodes.isEmpty()) {
                breakpointStatus.setVerified(false);
                breakpointStatus.setMessage("Unreachable");
            } else {
                breakpointStatus.setVerified(true);
            }
        }

        int startIndex;
        for (startIndex = 0; startIndex < breakpoints.size(); startIndex++) {
            if (breakpoints.get(startIndex).cfgNode().location().getFile().equals(goblintSourcePath)) {
                break;
            }
        }
        breakpoints.removeIf(b -> b.cfgNode().location().getFile().equals(goblintSourcePath));
        breakpoints.addAll(startIndex, newBreakpoints);

        var response = new SetBreakpointsResponse();
        response.setBreakpoints(newBreakpointStatuses.toArray(Breakpoint[]::new));
        return CompletableFuture.completedFuture(response);
    }

    /**
     * DAP request to set exception breakpoints.
     * Note: This should not be called by the IDE given our reported capabilities, but VSCode calls it anyway.
     * It is implemented as a no-op to avoid errors.
     */
    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        var response = new SetExceptionBreakpointsResponse();
        response.setBreakpoints(new Breakpoint[0]);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Finds target nodes having the given CFG node and matching the conditional expression if provided.
     *
     * @throws IllegalArgumentException if evaluating the condition failed.
     */
    private List<NodeInfo> findTargetNodes(CFGNodeInfo cfgNode, @Nullable ConditionalExpression condition) {
        var candidateNodes = resultsService.lookupNodes(LookupParams.byCFGNodeId(cfgNode.cfgNodeId()));
        if (condition == null) {
            return candidateNodes;
        } else {
            return candidateNodes.stream()
                    .filter(n -> condition.evaluateCondition(n, resultsService))
                    .toList();
        }
    }

    /**
     * Notifies the debugger that all initial configuration requests have been made.
     * Launching waits for this to arrive before starting the debugger.
     */
    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        log.info("Debug adapter configuration done");
        configurationDoneFuture.complete(null);
        configurationDoneFuture = new CompletableFuture<>();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * DAP request to attach to debugger.
     * Note: Attach doesn't make sense for abstract debugging, but to avoid issues in case the client requests it anyway we just treat it as a launch request.
     */
    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return launch(args);
    }

    /**
     * DAP request to launch debugger.
     * Launches the abstract debugger and runs to first breakpoint. Waits for configuration before launching.
     */
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

    /**
     * Disconnects the debugger. For abstract debugging this is a no-op.
     */
    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Runs to next breakpoint.
     * Note: Breakpoints are run in the order in which the client sent them, not in any content based ordering.
     * In VS Code breakpoints appear to be sent in the order of their line numbers.
     */
    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        runToNextBreakpoint(1);
        return CompletableFuture.completedFuture(new ContinueResponse());
    }

    /**
     * Runs to previous breakpoint.
     */
    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        runToNextBreakpoint(-1);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * DAP request to pause a thread. Abstract debugger threads are always paused so this is a no-op.
     */
    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Restarts the given frame at the start of the function. In VS Code this can be accessed as an icon on the right side of the stack frame in the call stacks view.
     */
    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        int targetThreadId = getThreadId(args.getFrameId());
        int targetFrameIndex = getFrameIndex(args.getFrameId());
        try {
            stepAllThreadsToMatchingFrame(targetThreadId, targetFrameIndex, true);
            return CompletableFuture.completedFuture(null);
        } catch (IllegalStepException e) {
            return CompletableFuture.failedFuture(userFacingError("Cannot restart frame. " + e.getMessage()));
        }
    }

    /**
     * Terminates (removes) a thread. In VS Code this can be accessed by right-clicking on a thread in the call stacks view.
     */
    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        for (int threadId : args.getThreadIds()) {
            threads.remove(threadId);
        }
        for (int threadId : args.getThreadIds()) {
            var event = new ThreadEventArguments();
            event.setReason("exited");
            event.setThreadId(threadId);
            client.thread(event);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns list of valid targets for step in operation. (In VS Code this is used by step into targets).
     */
    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        ThreadState currentThread = threads.get(getThreadId(args.getFrameId()));
        NodeInfo currentNode = currentThread.getCurrentFrame().getNode();

        List<StepInTarget> targets = new ArrayList<>();
        if (currentNode != null) {
            List<StepInTarget> forwardTargets = new ArrayList<>();

            {
                var entryEdges = currentNode.outgoingEntryEdges();
                for (int i = 0; i < entryEdges.size(); i++) {
                    var edge = entryEdges.get(i);
                    forwardTargets.add(target(
                            STEP_IN_OFFSET + i,
                            "Step in: " + (edge.createsNewThread() ? "thread " : "call ") + edge.function() + "(" + String.join(", ", edge.args()) + ")",
                            currentNode.location()
                    ));
                }
            }

            // Only show CFG edges as step in targets if there is no stepping over function calls and there is branching
            if (currentNode.outgoingEntryEdges().isEmpty() && currentNode.outgoingCFGEdges().size() > 1) {
                var cfgEdges = currentNode.outgoingCFGEdges();
                for (int i = 0; i < cfgEdges.size(); i++) {
                    var edge = cfgEdges.get(i);
                    var node = resultsService.lookupNode(edge.nodeId());
                    forwardTargets.add(target(
                            STEP_OVER_OFFSET + i,
                            "Step: " + edge.statementDisplayString(),
                            node.location()
                    ));
                }
            }

            // Sort forward stepping targets by the order they appear in code
            forwardTargets.sort(Comparator.comparing(StepInTarget::getLine).thenComparing(StepInTarget::getColumn));
            targets.addAll(forwardTargets);

            // Backward stepping entry targets are not sorted, to ensure their order matches the order of stack frames
            if (currentThread.hasPreviousFrame() && currentThread.getPreviousFrame().isAmbiguousFrame()) {
                var frames = currentThread.getFrames();
                for (int i = 1; i < frames.size(); i++) {
                    var node = frames.get(i).getNode();
                    assert node != null; // Ambiguous frames can't be unavailable
                    targets.add(target(
                            STEP_BACK_OUT_OFFSET + i,
                            "Step back: " + node.function() + " " + node.nodeId(),
                            node.location()
                    ));
                }
            }

            List<StepInTarget> backwardTargets = new ArrayList<>();

            if (currentNode.incomingCFGEdges().size() > 1) {
                var cfgEdges = currentNode.incomingCFGEdges();
                for (int i = 0; i < cfgEdges.size(); i++) {
                    var edge = cfgEdges.get(i);
                    var node = resultsService.lookupNode(edge.nodeId());
                    backwardTargets.add(target(
                            STEP_BACK_OVER_OFFSET + i,
                            "Step back: " + edge.statementDisplayString(),
                            node.location()
                    ));
                }
            }

            // Sort backward stepping CFG targets by the order they appear in code
            backwardTargets.sort(Comparator.comparing(StepInTarget::getLine).thenComparing(StepInTarget::getColumn));
            targets.addAll(backwardTargets);
        }

        var response = new StepInTargetsResponse();
        response.setTargets(targets.toArray(StepInTarget[]::new));
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Helper method to create StepInTarget.
     */
    private StepInTarget target(int id, String label, GoblintLocation location) {
        var target = new StepInTarget();
        target.setId(id);
        target.setLabel(label);
        target.setLine(location.getLine());
        target.setColumn(location.getColumn());
        target.setEndLine(location.getEndLine());
        target.setEndColumn(location.getEndColumn());
        return target;
    }

    /**
     * DAP next operation. In VS Code this corresponds to step over.
     */
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
            NodeInfo node = thread.getCurrentFrame().getNode();
            if (node != null && node.outgoingCFGEdges().size() > 1 && !node.outgoingEntryEdges().isEmpty()) {
                return CompletableFuture.failedFuture(userFacingError("Ambiguous path through function" + (thread == targetThread ? "" : " for " + thread.getName()) +
                        ". Step into function to choose the desired path."));
            }
        }
        if (currentNode.outgoingCFGEdges().size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Branching control flow. Use step into target to choose the desired branch."));
        }
        return stepOver(args.getThreadId(), 0);
    }

    /**
     * DAP step in operation.
     * Allows explicit target selection by setting targetId. In VS Code this is the step into targets operation.
     */
    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        var currentNode = threads.get(args.getThreadId()).getCurrentFrame().getNode();
        if (currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError((args.getTargetId() == null ? "Cannot step in." : "Cannot step to target.") + " Location is unavailable."));
        }

        if (args.getTargetId() == null) {
            // Normal step in operation
            if (currentNode.outgoingEntryEdges().isEmpty()) {
                var nextArgs = new NextArguments();
                nextArgs.setThreadId(args.getThreadId());
                nextArgs.setSingleThread(args.getSingleThread());
                nextArgs.setGranularity(args.getGranularity());
                return next(nextArgs);
            } else if (currentNode.outgoingEntryEdges().size() > 1) {
                return CompletableFuture.failedFuture(userFacingError("Ambiguous function call. Use step into target to choose the desired call"));
            }
            return stepIn(args.getThreadId(), 0);
        } else {
            // Step into targets operation
            int targetId = args.getTargetId();
            if (targetId >= STEP_BACK_OUT_OFFSET) {
                int targetIndex = targetId - STEP_BACK_OUT_OFFSET;
                return stepBackOut(args.getThreadId(), targetIndex);
            } else if (targetId >= STEP_BACK_OVER_OFFSET) {
                int targetIndex = targetId - STEP_BACK_OVER_OFFSET;
                return stepBackOver(args.getThreadId(), targetIndex);
            } else if (targetId >= STEP_IN_OFFSET) {
                int targetIndex = targetId - STEP_IN_OFFSET;
                return stepIn(args.getThreadId(), targetIndex);
            } else if (targetId >= STEP_OVER_OFFSET) {
                int targetIndex = targetId - STEP_OVER_OFFSET;
                return stepOver(args.getThreadId(), targetIndex);
            } else {
                return CompletableFuture.failedFuture(new IllegalStateException("Unknown step in target: " + targetId));
            }
        }
    }

    /**
     * DAP step out operation.
     */
    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        ThreadState targetThread = threads.get(args.getThreadId());
        if (targetThread.getCurrentFrame().getNode() == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Location is unavailable."));
        } else if (!targetThread.hasPreviousFrame()) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Reached top of call stack."));
        } else if (targetThread.getPreviousFrame().isAmbiguousFrame()) {
            // Restart frame isn't equivalent to step out, it moves you to the start of the frame, which means you have to step to your target location manually.
            // TODO: Find/create a better alternative for stepping out with ambiguous caller.
            return CompletableFuture.failedFuture(userFacingError("Ambiguous caller frame. Use restart frame to choose the desired frame."));
        }

        NodeInfo targetCallNode = targetThread.getPreviousFrame().getNode();
        assert targetCallNode != null;
        if (targetCallNode.outgoingCFGEdges().isEmpty()) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step out. Function never returns."));
        }

        return stepOut(args.getThreadId());
    }

    /**
     * DAP step back operation.
     */
    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        var targetThread = threads.get(args.getThreadId());
        var currentNode = targetThread.getCurrentFrame().getNode();
        if (currentNode == null) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. Location is unavailable."));
        } else if (currentNode.incomingCFGEdges().isEmpty()) {
            // Reached start of function
            if (!targetThread.hasPreviousFrame()) {
                return CompletableFuture.failedFuture(userFacingError("Cannot step back. Reached start of program."));
            } else if (targetThread.getPreviousFrame().isAmbiguousFrame()) {
                return CompletableFuture.failedFuture(userFacingError("Ambiguous previous frame. Use step into target to choose desired frame."));
            }
            return stepBackOut(args.getThreadId(), 1);
        } else if (currentNode.incomingCFGEdges().size() > 1) {
            return CompletableFuture.failedFuture(userFacingError("Ambiguous previous location. Use step into target to choose desired location."));
        }

        return stepBackOver(args.getThreadId(), 0);
    }

    // Concrete implementations of step operations. These are called from the respective requests as well as from stepIn if a corresponding target is requested.

    /**
     * Implements step over for a specific target edge.
     *
     * @param targetIndex index of the target edge
     */
    private CompletableFuture<Void> stepOver(int targetThreadId, int targetIndex) {
        NodeInfo currentNode = threads.get(targetThreadId).getCurrentFrame().getNode();
        assert currentNode != null;
        try {
            var targetEdge = currentNode.outgoingCFGEdges().get(targetIndex);
            stepAllThreadsOverMatchingEdge(targetThreadId, targetEdge, NodeInfo::outgoingCFGEdges);
            return CompletableFuture.completedFuture(null);
        } catch (IllegalStepException e) {
            // Log error because if 'Step into target' menu is open then errors returned by this function are not shown in VSCode.
            // TODO: Open issue about this in VSCode issue tracker.
            log.error("Cannot step over. " + e.getMessage());
            return CompletableFuture.failedFuture(userFacingError("Cannot step over. " + e.getMessage()));
        }
    }

    /**
     * Implements step in for a specific target edge.
     *
     * @param targetIndex index of the target edge
     */
    private CompletableFuture<Void> stepIn(int targetThreadId, int targetIndex) {
        NodeInfo currentNode = threads.get(targetThreadId).getCurrentFrame().getNode();
        assert currentNode != null;
        try {
            var targetEdge = currentNode.outgoingEntryEdges().get(targetIndex);
            stepAllThreadsIntoMatchingEdge(targetThreadId, targetEdge, NodeInfo::outgoingEntryEdges);
            return CompletableFuture.completedFuture(null);
        } catch (IllegalStepException e) {
            // Log error because if 'Step into target' menu is open then errors returned by this function are not shown in VSCode.
            // TODO: Open issue about this in VSCode issue tracker.
            log.error("Cannot step in. " + e.getMessage());
            return CompletableFuture.failedFuture(userFacingError("Cannot step in. " + e.getMessage()));
        }
    }

    /**
     * Implements step out. Assumes that the target thread has already been checked and is known to be available and have a previous frame.
     */
    private CompletableFuture<Void> stepOut(int targetThreadId) {
        ThreadState targetThread = threads.get(targetThreadId);
        NodeInfo targetCallNode = targetThread.getPreviousFrame().getNode();
        assert targetCallNode != null;

        Map<Integer, NodeInfo> targetNodes = new HashMap<>();
        for (var threadEntry : threads.entrySet()) {
            int threadId = threadEntry.getKey();
            ThreadState thread = threadEntry.getValue();

            // Skip all threads that have no known previous frame or whose previous frame has a different location compared to the target thread.
            // Note that threads with an unavailable current or previous frame are kept.
            if (!thread.hasPreviousFrame() || thread.getPreviousFrame().isAmbiguousFrame()
                    || (thread.getPreviousFrame().getNode() != null && !Objects.equals(thread.getPreviousFrame().getNode().cfgNodeId(), targetCallNode.cfgNodeId()))) {
                continue;
            }

            NodeInfo currentNode = thread.getCurrentFrame().getNode();
            NodeInfo targetNode;
            if (currentNode == null) {
                targetNode = null;
            } else {
                Predicate<String> filter;
                if (thread.getCurrentFrame().getLocalThreadIndex() != thread.getPreviousFrame().getLocalThreadIndex()) {
                    // If thread exit then control flow will not return to parent frame. No information to filter with so simply allow all possible nodes.
                    filter = _id -> true;
                } else {
                    // If not thread exit then filter possible nodes after function call in parent frame to those that are also possible return targets of current frame.
                    Set<String> returnNodeIds = findMatchingNodes(currentNode, NodeInfo::outgoingCFGEdges, e -> !e.outgoingReturnEdges().isEmpty()).stream()
                            .flatMap(n -> n.outgoingReturnEdges().stream())
                            .map(EdgeInfo::nodeId)
                            .collect(Collectors.toSet());
                    filter = returnNodeIds::contains;
                }

                NodeInfo currentCallNode = thread.getPreviousFrame().getNode();
                List<String> candidateTargetNodeIds = currentCallNode.outgoingCFGEdges().stream()
                        .map(EdgeInfo::nodeId)
                        .filter(filter)
                        .toList();

                if (candidateTargetNodeIds.isEmpty()) {
                    targetNode = null;
                } else if (candidateTargetNodeIds.size() == 1) {
                    targetNode = resultsService.lookupNode(candidateTargetNodeIds.get(0));
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

        onThreadsStopped("step", targetThreadId);

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> stepBackOver(int targetThreadId, int targetIndex) {
        NodeInfo currentNode = threads.get(targetThreadId).getCurrentFrame().getNode();
        assert currentNode != null;
        EdgeInfo targetEdge = currentNode.incomingCFGEdges().get(targetIndex);
        try {
            stepAllThreadsOverMatchingEdge(targetThreadId, targetEdge, NodeInfo::incomingCFGEdges);
            return CompletableFuture.completedFuture(null);
        } catch (IllegalStepException e) {
            // Log error because if 'Step into target' menu is open then errors returned by this function are not shown in VSCode.
            // TODO: Open issue about this in VSCode issue tracker.
            log.error("Cannot step back. " + e.getMessage());
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. " + e.getMessage()));
        }
    }

    private CompletableFuture<Void> stepBackOut(int targetThreadId, int targetIndex) {
        try {
            stepAllThreadsToMatchingFrame(targetThreadId, targetIndex, false);
            return CompletableFuture.completedFuture(null);
        } catch (IllegalStepException e) {
            return CompletableFuture.failedFuture(userFacingError("Cannot step back. " + e.getMessage()));
        }
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
                targetNodes = resultsService.lookupNodes(LookupParams.entryPoint());
            } else {
                var breakpoint = breakpoints.get(activeBreakpoint);
                stopReason = "breakpoint";
                targetLocation = breakpoint.cfgNode().location();
                targetNodes = breakpoint.targetNodes();
            }

            if (!targetNodes.isEmpty()) {
                setThreads(
                        targetNodes.stream()
                                .map(node -> new ThreadState("breakpoint " + node.nodeId(), assembleStackTrace(node)))
                                .toList()
                );

                onThreadsStopped(stopReason, threads.keySet().stream().findFirst().orElseThrow());

                log.info("Stopped on breakpoint " + activeBreakpoint + " (" + targetLocation + ")");
                return;
            }

            log.info("Skipped unreachable breakpoint " + activeBreakpoint + " (" + targetLocation + ")");
        }

        log.info("All breakpoints visited. Terminating debugger.");
        var event = new TerminatedEventArguments();
        client.terminated(event);
    }

    /**
     * Steps all threads along an edge matching primaryTargetEdge.
     * Edges are matched by ARG node. If no edge with matching ARG node is found then edges are matched by CFG node.
     * If no edge with matching CFG node is found then thread becomes unavailable.
     *
     * @throws IllegalStepException if the target node is ambiguous ie there are multiple candidate edges that have the target CFG node.
     */
    private void stepAllThreadsOverMatchingEdge(int primaryThreadId, EdgeInfo primaryTargetEdge, Function<NodeInfo, List<? extends EdgeInfo>> getCandidateEdges)
            throws IllegalStepException {
        List<Pair<ThreadState, NodeInfo>> steps = new ArrayList<>();
        for (var thread : threads.values()) {
            StackFrameState currentFrame = thread.getCurrentFrame();

            NodeInfo targetNode;
            if (currentFrame.getNode() != null) {
                List<? extends EdgeInfo> candidateEdges = getCandidateEdges.apply(currentFrame.getNode());
                EdgeInfo targetEdge = findTargetEdge(primaryTargetEdge, candidateEdges, thread.getName());
                targetNode = targetEdge == null ? null : resultsService.lookupNode(targetEdge.nodeId());
            } else if (currentFrame.getLastReachableNode() != null && currentFrame.getLastReachableNode().cfgNodeId().equals(primaryTargetEdge.cfgNodeId())) {
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

        onThreadsStopped("step", primaryThreadId);
    }

    /**
     * Steps all threads along an edge matching primaryTargetEdge and adds a new stack frame with the target node.
     * Edges are matched by ARG node. If no edge with matching ARG node is found then edges are matched by CFG node.
     * If no edge with matching CFG node is found then thread becomes unavailable.
     *
     * @throws IllegalStepException if the target node is ambiguous ie there are multiple candidate edges that have the target CFG node.
     */
    private void stepAllThreadsIntoMatchingEdge(int primaryThreadId, EdgeInfo primaryTargetEdge, Function<NodeInfo, List<? extends EdgeInfo>> getCandidateEdges)
            throws IllegalStepException {
        // Note: It is important that all threads, including threads with unavailable location, are stepped, because otherwise the number of added stack frames will get out of sync.
        List<Pair<ThreadState, EdgeInfo>> steps = new ArrayList<>();
        for (var thread : threads.values()) {
            StackFrameState currentFrame = thread.getCurrentFrame();

            EdgeInfo targetEdge;
            if (currentFrame.getNode() != null) {
                List<? extends EdgeInfo> candidateEdges = getCandidateEdges.apply(currentFrame.getNode());
                targetEdge = findTargetEdge(primaryTargetEdge, candidateEdges, thread.getName());
            } else {
                targetEdge = null;
            }

            steps.add(Pair.of(thread, targetEdge));
        }

        for (var step : steps) {
            ThreadState thread = step.getLeft();
            EdgeInfo targetEdge = step.getRight();
            NodeInfo targetNode = resultsService.lookupNode(targetEdge.nodeId());
            boolean isNewThread = targetEdge instanceof FunctionCallEdgeInfo fce && fce.createsNewThread();
            thread.pushFrame(new StackFrameState(targetNode, false, thread.getCurrentFrame().getLocalThreadIndex() - (isNewThread ? 1 : 0)));
        }

        onThreadsStopped("step", primaryThreadId);
    }

    private EdgeInfo findTargetEdge(EdgeInfo primaryTargetEdge, List<? extends EdgeInfo> candidateEdges, String threadName) throws IllegalStepException {
        // This is will throw if there are multiple distinct target edges with the same target CFG node.
        // TODO: Somehow ensure this can never happen.
        //  Options:
        //  * Throw error (current approach) (problem: might make it impossible to step at all in some cases. it is difficult to provide meaningful error messages for all cases)
        //  * Split thread into multiple threads. (problem: complicates 'step back' and maintaining thread ordering)
        //  * Identify true source of branching and use it to disambiguate (problem: there might not be a source of branching in all cases. complicates stepping logic)
        //  * Make ambiguous threads unavailable (problem: complicates mental model of when threads become unavailable.)

        EdgeInfo targetEdgeByARGNode = candidateEdges.stream()
                .filter(e -> e.nodeId().equals(primaryTargetEdge.nodeId()))
                .findAny().orElse(null);
        if (targetEdgeByARGNode != null) {
            return targetEdgeByARGNode;
        }
        List<? extends EdgeInfo> targetEdgesByCFGNode = candidateEdges.stream()
                .filter(e -> e.cfgNodeId().equals(primaryTargetEdge.cfgNodeId()))
                .toList();
        if (targetEdgesByCFGNode.size() > 1) {
            throw new IllegalStepException("Path is ambiguous for " + threadName + ".");
        }
        return targetEdgesByCFGNode.size() == 1 ? targetEdgesByCFGNode.get(0) : null;
    }

    /**
     * Moves all threads to a matching frame. Frame is matched by frame index (position counting from topmost frame) and CFG node.
     *
     * @param restart if true, moves to the start of the frame, otherwise preserves current position in frame
     * @throws IllegalStepException if the primary thread target frame is unavailable or the target frame is ambiguous for some thread.
     */
    private void stepAllThreadsToMatchingFrame(int primaryThreadId, int primaryTargetFrameIndex, boolean restart) throws IllegalStepException {
        ThreadState targetThread = threads.get(primaryThreadId);

        int targetPosition = primaryTargetFrameIndex;
        while (targetPosition > 0 && targetThread.getFrames().get(targetPosition - 1).isAmbiguousFrame()) {
            targetPosition -= 1;
        }

        StackFrameState targetFrame = targetThread.getFrames().get(primaryTargetFrameIndex);
        if (targetFrame.getNode() == null) {
            throw new IllegalStepException("Target frame is unavailable.");
        }
        String targetCFGId = targetFrame.getNode().cfgNodeId();

        Map<Integer, Integer> frameIndexes = new HashMap<>();
        for (var threadEntry : threads.entrySet()) {
            Integer frameIndex;
            if (threadEntry.getValue() == targetThread) {
                frameIndex = primaryTargetFrameIndex;
            } else {
                try {
                    frameIndex = findFrameIndex(threadEntry.getValue().getFrames(), targetPosition, targetCFGId);
                } catch (IllegalStateException e) {
                    throw new IllegalStepException("Ambiguous target frame for " + threadEntry.getValue().getName() + ".");
                }
            }

            if (frameIndex != null) {
                frameIndexes.put(threadEntry.getKey(), frameIndex);
            }
        }

        threads.keySet().removeIf(t -> !frameIndexes.containsKey(t));
        for (var threadEntry : threads.entrySet()) {
            int threadId = threadEntry.getKey();
            ThreadState thread = threadEntry.getValue();

            int frameIndex = frameIndexes.get(threadId);
            // Remove all frames on top of the target frame
            thread.getFrames().subList(0, frameIndex).clear();
            if (thread.getCurrentFrame().isAmbiguousFrame()) {
                // If the target frame is ambiguous then rebuild stack
                List<StackFrameState> newStackTrace = assembleStackTrace(thread.getCurrentFrame().getNode());
                thread.getFrames().clear();
                thread.getFrames().addAll(newStackTrace);
            }
            if (restart) {
                NodeInfo startNode = thread.getCurrentFrame().getNode() != null ? thread.getCurrentFrame().getNode() : thread.getCurrentFrame().getLastReachableNode();
                if (startNode != null) {
                    thread.getCurrentFrame().setNode(getEntryNode(startNode));
                }
            }
        }

        onThreadsStopped("step", primaryThreadId);
    }

    /**
     * Helper method for {@link #stepAllThreadsToMatchingFrame}.
     * <p>
     * Finds the matching frame for the given call stack and returns its index.
     * Returns null if there is no matching frame, either because the desired index is out of range or does not have the desired CFG node.
     * Note that unavailable frames are considered matching, on the assumption that for them to become unavailable
     * they must have had a matching CFG node with other threads at some point in the past.
     */
    private Integer findFrameIndex(List<StackFrameState> frames, int targetPosition, String targetCFGNodeId) {
        // When restarting the frame it might make more sense to compare entry nodes rather than current nodes,
        // however, this can cause unexpected ambiguities when there are ambiguous frames with the same entry node but different current nodes.
        // TODO: Make an explicit and reasoned decision on this.
        if (frames.size() <= targetPosition) {
            return null;
        }
        if (frames.get(targetPosition).isAmbiguousFrame()) {
            Integer foundIndex = null;
            for (int i = targetPosition; i < frames.size(); i++) {
                var frame = frames.get(i);
                assert frame.getNode() != null; // It should be impossible for ambiguous frames to be unavailable.
                if (frame.getNode().cfgNodeId().equals(targetCFGNodeId)) {
                    if (foundIndex != null) {
                        throw new IllegalStateException("Ambiguous target frame");
                    }
                    foundIndex = i;
                }
            }
            return foundIndex;
        } else {
            var frame = frames.get(targetPosition);
            // Preserve unavailable frames because otherwise threads could be spuriously lost
            if (frame.getNode() == null || frame.getNode().cfgNodeId().equals(targetCFGNodeId)) {
                return targetPosition;
            }
            return null;
        }
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

    /**
     * Returns the stack trace for the given thread.
     */
    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        var thread = threads.get(args.getThreadId());

        final int currentThreadId = thread.getCurrentFrame().getLocalThreadIndex();
        StackFrame[] stackFrames = new StackFrame[thread.getFrames().size()];
        for (int i = 0; i < thread.getFrames().size(); i++) {
            var frame = thread.getFrames().get(i);

            var stackFrame = new StackFrame();
            stackFrame.setId(getFrameId(args.getThreadId(), i));
            // TODO: Notation for ambiguous frames and parent threads could be clearer.
            if (frame.getNode() != null) {
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
            } else {
                stackFrame.setName("No matching path");
            }

            stackFrames[i] = stackFrame;
        }

        var response = new StackTraceResponse();
        response.setStackFrames(stackFrames);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Returns variable scopes for the given stack frame.
     */
    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        var frame = getFrame(args.getFrameId());
        if (frame.getNode() == null) {
            throw new IllegalStateException("Attempt to request variables for unavailable frame " + args.getFrameId());
        }

        Scope[] scopes = nodeScopes.computeIfAbsent(frame.getNode().nodeId(), nodeId -> {
            NodeInfo currentNode = frame.getNode();

            JsonObject state = resultsService.lookupState(currentNode.nodeId());
            JsonElement globalState = resultsService.lookupGlobalState();
            Map<String, GoblintVarinfo> varinfos = resultsService.getVarinfos().stream()
                    .filter(v -> (v.getFunction() == null || v.getFunction().equals(currentNode.function())) && !"function".equals(v.getRole()))
                    .collect(Collectors.toMap(GoblintVarinfo::getName, v -> v));

            List<Variable> localVariables = new ArrayList<>();
            List<Variable> globalVariables = new ArrayList<>();

            if (state.has("threadflag")) {
                globalVariables.add(domainValueToVariable("<threadflag>", "(analysis threading mode)", state.get("threadflag")));
            }
            if (state.has("mutex")) {
                globalVariables.add(domainValueToVariable("<mutex>", "(set of unique locked mutexes)", state.get("mutex")));
            }
            if (state.has("symb_locks")) {
                globalVariables.add(domainValueToVariable("<symb_locks>", "(set of locked mutexes tracked by symbolic references)", state.get("symb_locks")));
            }

            JsonObject domainValues = state.get("base").getAsJsonObject().get("value domain").getAsJsonObject();

            // Add special values.
            for (var entry : domainValues.entrySet()) {
                if (varinfos.containsKey(entry.getKey()) || entry.getKey().startsWith("((alloc")) {
                    // Hide normal variables because they are added later.
                    // Hide allocations because they require manually matching identifiers to interpret.
                    continue;
                }
                // In most cases the only remaining value is RETURN. Consider it local.
                // TODO: RETURN special value can end up in globals if there is also a global variable RETURN. This needs changes on the Goblint side to fix.
                localVariables.add(domainValueToVariable("(" + entry.getKey() + ")", "(special value)", entry.getValue()));
            }

            // Add variables.
            for (var varinfo : varinfos.values()) {
                if (varinfo.getOriginalName() == null || (varinfo.getFunction() == null && STD_VARIABLES.contains(varinfo.getOriginalName()))) {
                    // Hide synthetic variables because they are impossible to interpret without looking at the CFG.
                    // Hide global built-in and standard library variables because they are generally irrelevant and not used in the analysis.
                    continue;
                }

                String name = varinfo.getName().equals(varinfo.getOriginalName())
                        ? varinfo.getName()
                        : varinfo.getOriginalName() + " (" + varinfo.getName() + ")";
                JsonElement value = domainValues.get(varinfo.getName());
                if (value == null) {
                    if (varinfo.getFunction() != null) {
                        // Skip local variables that are not present in base domain, because this generally means we are on a special ARG node where local variables are not tracked.
                        continue;
                    }
                    // If domain does not contain variable value use Goblint to evaluate the value.
                    // This generally happens for global variables in multithreaded mode.
                    value = resultsService.evaluateExpression(currentNode.nodeId(), varinfo.getName());
                }

                List<Variable> scope = varinfo.getFunction() == null ? globalVariables : localVariables;

                scope.add(domainValueToVariable(name, varinfo.getType(), value));
            }

            List<Variable> rawVariables = new ArrayList<>();
            rawVariables.add(domainValueToVariable("(local-state)", "local state; result of arg/state request", state));
            rawVariables.add(domainValueToVariable("(global-state)", "global state; result of global-state request", globalState));

            return new Scope[]{
                    scope("Local", localVariables),
                    scope("Global", globalVariables),
                    scope("Raw", rawVariables)
            };
        });

        var response = new ScopesResponse();
        response.setScopes(scopes);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Returns variables for the given variable reference (a variable reference is generally a variable scope or a complex variable).
     */
    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        var response = new VariablesResponse();
        response.setVariables(getVariables(args.getVariablesReference()));
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Evaluates the given expression and returns the result.
     */
    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        var frame = getFrame(args.getFrameId());
        if (frame.getNode() == null) {
            throw new IllegalStateException("Attempt to evaluate expression in unavailable frame " + args.getFrameId());
        }

        JsonElement result;
        try {
            if (ConditionalExpression.hasExplicitMode(args.getExpression())) {
                // If explicit mode is set then defer to ConditionalExpression for evaluation.
                result = ConditionalExpression.fromString(args.getExpression(), false)
                        .evaluateValue(frame.getNode(), resultsService);
            } else {
                // If explicit mode is not set evaluate as a C expression using Goblint.
                result = resultsService.evaluateExpression(frame.getNode().nodeId(), args.getExpression());
            }
        } catch (RequestFailedException | IllegalArgumentException e) {
            return CompletableFuture.failedFuture(userFacingError(e.getMessage()));
        }

        var response = new EvaluateResponse();
        var resultVariable = domainValueToVariable("", null, result);
        response.setResult(resultVariable.getValue());
        response.setVariablesReference(resultVariable.getVariablesReference());
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Converts a Goblint domain value into a DAP variable.
     * Note: Variables may contain variable references. Variable references are only valid until the next step.
     */
    private Variable domainValueToVariable(String name, @Nullable String type, JsonElement value) {
        if (value.isJsonObject()) {
            return compoundVariable(
                    name,
                    type,
                    false,
                    value.getAsJsonObject().entrySet().stream()
                            .map(f -> domainValueToVariable(f.getKey(), null, f.getValue()))
                            .toArray(Variable[]::new)
            );
        } else if (value.isJsonArray()) {
            var valueArray = value.getAsJsonArray();
            // Integer domains are generally represented as an array of 1-4 strings.
            // We want to display that as a non-compound variable for compactness and readability.
            // As a general heuristic, only arrays containing compound values or longer than 4 elements are displayed as compound variables.
            boolean displayAsCompound;
            if (valueArray.size() > 4) {
                displayAsCompound = true;
            } else {
                displayAsCompound = false;
                for (JsonElement jsonElement : valueArray) {
                    if (!jsonElement.isJsonPrimitive()) {
                        displayAsCompound = true;
                        break;
                    }
                }
            }

            if (displayAsCompound) {
                return compoundVariable(
                        name,
                        type,
                        true,
                        IntStream.range(0, valueArray.size())
                                .mapToObj(i -> domainValueToVariable(Integer.toString(i), null, valueArray.get(i)))
                                .toArray(Variable[]::new)
                );
            }
        }
        return variable(name, type, domainValueToString(value));
    }

    /**
     * Converts a Goblint domain value into a string.
     */
    private static String domainValueToString(JsonElement value) {
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        } else if (value.isJsonArray()) {
            return "[" + StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                    .map(AbstractDebuggingServer::domainValueToString)
                    .collect(Collectors.joining(", ")) + "]";
        } else if (value.isJsonObject()) {
            return "{" + value.getAsJsonObject().entrySet().stream()
                    .map(e -> e.getKey() + ": " + domainValueToString(e.getValue()))
                    .collect(Collectors.joining(", ")) + "}";
        } else {
            throw new IllegalArgumentException("Unknown domain value type: " + value.getClass());
        }
    }

    /**
     * Convenience function to construct a DAP scope.
     */
    private Scope scope(String name, List<Variable> variables) {
        Scope scope = new Scope();
        scope.setName(name);
        scope.setVariablesReference(storeVariables(variables.toArray(Variable[]::new)));
        return scope;
    }

    /**
     * Convenience function to construct a DAP compound variable.
     * Note: The given fields are stored as variable references. Variable references are only valid until the next step.
     */
    private Variable compoundVariable(String name, @Nullable String type, boolean isArray, Variable... fields) {
        Variable variable = new Variable();
        variable.setName(name);
        variable.setType(type);
        variable.setValue(compoundVariablePreview(isArray, fields));
        if (fields.length > 0) {
            variable.setVariablesReference(storeVariables(fields));
        }
        return variable;
    }

    /**
     * Constructs a preview string for a compound variable.
     */
    private static String compoundVariablePreview(boolean isArray, Variable... fields) {
        if (fields.length == 0) {
            return isArray ? "[]" : "{}";
        }
        if (isArray) {
            return "[" + fields[0].getValue() + (fields.length > 1 ? ", …" : "") + "]";
        } else {
            return "{" + Arrays.stream(fields)
                    .map(f -> f.getName() + ": " + (f.getVariablesReference() == 0 ? f.getValue() : "…"))
                    .collect(Collectors.joining(", ")) + "}";
        }
    }

    /**
     * Convenience function to construct a DAP variable.
     */
    private static Variable variable(String name, @Nullable String type, String value) {
        Variable variable = new Variable();
        variable.setName(name);
        variable.setType(type);
        variable.setValue(value);
        return variable;
    }

    // Helper methods:

    /**
     * Get stack frame by frame id.
     */
    private StackFrameState getFrame(int frameId) {
        int threadId = getThreadId(frameId);
        int frameIndex = getFrameIndex(frameId);
        return threads.get(threadId).getFrames().get(frameIndex);
    }

    /**
     * Construct stack frame id from thread id and frame index.
     */
    private static int getFrameId(int threadId, int frameIndex) {
        return threadId * FRAME_ID_THREAD_ID_MULTIPLIER + frameIndex;
    }

    /**
     * Extract thread id from frame id.
     */
    private int getThreadId(int frameId) {
        return frameId / FRAME_ID_THREAD_ID_MULTIPLIER;
    }

    /**
     * Extract frame index from frame id.
     */
    private int getFrameIndex(int frameId) {
        return frameId % FRAME_ID_THREAD_ID_MULTIPLIER;
    }

    private void setThreads(List<ThreadState> newThreads) {
        threads.clear();
        for (int i = 0; i < newThreads.size(); i++) {
            threads.put(i, newThreads.get(i));
        }
    }

    private Variable[] getVariables(int variablesReference) {
        return storedVariables.get(variablesReference - 1);
    }

    private int storeVariables(Variable[] variables) {
        storedVariables.add(variables);
        return storedVariables.size();
    }

    /**
     * Logic that should run every time after threads have stopped after a step or breakpoint.
     * Notifies client that threads have stopped and clears caches that should be invalidated whenever thread state changes.)
     */
    private void onThreadsStopped(String stopReason, int primaryThreadId) {
        storedVariables.clear();
        nodeScopes.clear();

        var event = new StoppedEventArguments();
        event.setReason(stopReason);
        event.setThreadId(primaryThreadId);
        event.setAllThreadsStopped(true);
        eventQueue.queue(() -> client.stopped(event));
    }

    /**
     * Logic to assemble a stack trace with the given start node as the topmost frame.
     */
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
                var node = resultsService.lookupNode(edge.nodeId());
                stackFrames.add(new StackFrameState(node, ambiguous, curThreadId));
            }
        } while (entryNode.incomingEntryEdges().size() == 1);
        return stackFrames;
    }

    /**
     * Finds the entry node for the function that contains the given ARG node.
     * The entry node is the first node of a function call.
     * The first node of a function call in the ARG should be a synthetic node added by the CIL and consequently should always be uniquely defined.
     */
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
            NodeInfo entryNode = _getEntryNode(resultsService.lookupNode(edge.nodeId()), seenNodes);
            if (entryNode != null) {
                return entryNode;
            }
        }
        return null;
    }

    /**
     * Finds all nodes matching the given condition that are inside the subgraph accessible
     * by repeatedly traversing edges returned by candidateEdges starting from the given node.
     */
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
            _findMatchingNodes(resultsService.lookupNode(edge.nodeId()), candidateEdges, condition, seenNodes, foundNodes);
        }
    }

    /**
     * Returns an exception that will be shown in the IDE as the message with no modifications and no additional context.
     */
    private ResponseErrorException userFacingError(String message) {
        return new ResponseErrorException(new ResponseError(ResponseErrorCode.RequestFailed, message, null));
    }

}
