package abstractdebugging;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public record ConditionalExpression(boolean must, String expression) {

    private static final String EXPLICIT_MODE_PREFIX = "\\";

    public static ConditionalExpression fromString(String conditionalExpression) {
        String mode, expression;
        if (hasExplicitMode(conditionalExpression)) {
            String[] parts = conditionalExpression.split("\\s+", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid expression: " + conditionalExpression);
            }
            mode = parts[0].substring(EXPLICIT_MODE_PREFIX.length());
            expression = parts[1];
        } else {
            mode = "may";
            expression = conditionalExpression;
        }
        return switch (mode) {
            case "may" -> new ConditionalExpression(false, expression);
            case "must" -> new ConditionalExpression(true, expression);
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }

    public static boolean hasExplicitMode(String conditionalExpression) {
        return conditionalExpression.startsWith(EXPLICIT_MODE_PREFIX);
    }

    /**
     * Evaluate expression as conditional at given node.
     *
     * @throws IllegalArgumentException if evaluating the condition failed.
     */
    public boolean evaluateCondition(NodeInfo node, ResultsService resultsService) {
        try {
            var result = resultsService.evaluateIntegerExpression(node.nodeId(), "!!(" + expression + ")");
            return must ? result.mustBeBool(true) : result.mayBeBool(true);
        } catch (RequestFailedException e) {
            throw new IllegalArgumentException("Error evaluating condition: " + e.getMessage());
        }
    }

    /**
     * Evaluate expression as value at given node.
     *
     * @throws IllegalArgumentException if evaluating the condition failed.
     */
    public JsonElement evaluateValue(NodeInfo node, ResultsService resultsService) {
        return new JsonPrimitive(evaluateCondition(node, resultsService));
    }

}
