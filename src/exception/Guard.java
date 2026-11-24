package exception;

public final class Guard {
    private Guard() {}

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) throw new ValidationException(field + " is required");
        return value;
    }

    public static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required");
        return value.trim();
    }

    public static int parseInt(String value, String field) {
        String s = requireNotBlank(value, field);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number");
        }
    }

    public static int parsePositiveInt(String value, String field) {
        return requirePositive(parseInt(value, field), field);
    }

    public static double parseNonNegativeDouble(String value, String field) {
        String s = requireNotBlank(value, field);
        try {
            return requireNonNegative(Double.parseDouble(s), field);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number");
        }
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) throw new ValidationException(field + " must be a positive number");
        return value;
    }

    public static double requireNonNegative(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0)
            throw new ValidationException(field + " must be a non-negative number");
        return value;
    }
}
