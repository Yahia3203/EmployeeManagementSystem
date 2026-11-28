package exception;

import java.util.regex.Pattern;

public final class Guard {
    public static final int MAX_AGE = 120;

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

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
        if (!INTEGER.matcher(s).matches()) {
            throw new ValidationException(field + " must be a whole number");
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " is out of range");
        }
    }

    public static int parsePositiveInt(String value, String field) {
        return requirePositive(parseInt(value, field), field);
    }

    public static double parseNonNegativeDouble(String value, String field) {
        String s = requireNotBlank(value, field);
        if (!DECIMAL.matcher(s).matches()) {
            throw new ValidationException(field + " must be a number");
        }
        return requireNonNegative(Double.parseDouble(s), field);
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) throw new ValidationException(field + " must be a positive number");
        return value;
    }

    public static int requireAge(int value, String field) {
        requirePositive(value, field);
        if (value > MAX_AGE) throw new ValidationException(field + " must not exceed " + MAX_AGE);
        return value;
    }

    public static double requireNonNegative(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0)
            throw new ValidationException(field + " must be a non-negative number");
        return value;
    }
}
