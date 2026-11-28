package exception;

public class ValidationException extends EmsException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) { super(message); }
}
