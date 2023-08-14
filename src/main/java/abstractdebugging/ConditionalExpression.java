package abstractdebugging;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/**
 * @since 0.0.4
 */

public record ConditionalExpression(boolean must, String expression) {

    private static final String EXPLICIT_MODE_PREFIX = "\\";

    /**
     * <p>
     * Creates a ConditionalExpression by parsing a string.
     * If useDefault is true then expressions without an explicit mode will default to mode may, otherwise an exception is thrown.
     * </p>
     * <p>
     * An expression with explicit mode takes the form {@code \<mode> <expression>}.
     * An expression without explicit mode takes the form {@code <expression>}.
     * </p>
     * <p>
     * The supported modes are:
     * <li>may - true if the given C expression may evaluate to true</li>
     * <li>must - true if the given C expression must evaluate to true</li>
     * </p>
     *
     * @throws IllegalArgumentException if parsing the expression fails.
     *                                  Note that evaluating the expression may also throw, so this method not throwing does not mean the expression is guaranteed to be valid.
     */
    public static ConditionalExpression fromString(String conditionalExpression, boolean useDefault) {
        String mode, expression;
        if (hasExplicitMode(conditionalExpression)) {
            String[] parts = conditionalExpression.split("\\s+", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid expression: " + conditionalExpression);
            }
            mode = parts[0].substring(EXPLICIT_MODE_PREFIX.length()).toLowerCase(Locale.ROOT);
            expression = parts[1];
        } else {
            if (!useDefault) {
                throw new IllegalArgumentException("Must specify mode explicitly");
            }
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
