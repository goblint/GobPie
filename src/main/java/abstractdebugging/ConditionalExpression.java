package abstractdebugging;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import javax.annotation.Nullable;
import java.util.Locale;

public record ConditionalExpression(boolean must, String expression) {

    private static final String EXPLICIT_MODE_PREFIX = "\\";

    public enum Mode {
        MAY,
        MUST;
    }

    public static ConditionalExpression fromString(String conditionalExpression, @Nullable Mode defaultMode) {
        Mode mode;
        String expression;
        if (hasExplicitMode(conditionalExpression)) {
            String[] parts = conditionalExpression.split("\\s+", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid expression: " + conditionalExpression);
            }
            mode = parseMode(parts[0].substring(EXPLICIT_MODE_PREFIX.length()));
            expression = parts[1];
        } else {
            if (defaultMode == null) {
                throw new IllegalArgumentException("Must specify mode explicitly");
            }
            mode = defaultMode;
            expression = conditionalExpression;
        }

        return switch (mode) {
            case MAY -> new ConditionalExpression(false, expression);
            case MUST -> new ConditionalExpression(true, expression);
        };
    }

    public static boolean hasExplicitMode(String conditionalExpression) {
        return conditionalExpression.startsWith(EXPLICIT_MODE_PREFIX);
    }

    private static Mode parseMode(String mode) {
        try {
            return Mode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
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
