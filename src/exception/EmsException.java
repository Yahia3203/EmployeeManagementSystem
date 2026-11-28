package exception;

public class EmsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmsException(String message) { super(message); }

    public EmsException(String message, Throwable cause) { super(message, cause); }
}
