package abstractdebugging;

public record ConditionalExpression(Mode mode, String expression) {

    public static ConditionalExpression fromString(String conditionalExpression) {
        if (conditionalExpression.startsWith("\\")) {
            String[] parts = conditionalExpression.split("\\s+", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid expression: " + conditionalExpression);
            }
            Mode mode = Mode.fromString(parts[0].substring(1));
            String expression = parts[1];
            return new ConditionalExpression(mode, expression);
        } else {
            return new ConditionalExpression(Mode.MAY, conditionalExpression);
        }
    }

    public enum Mode {
        MAY,
        MUST;

        public static Mode fromString(String value) {
            return switch (value) {
                case "may" -> MAY;
                case "must" -> MUST;
                default -> throw new IllegalArgumentException("Unknown mode: " + value);
            };
        }
    }

}
