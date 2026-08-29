package michal.radecki.request_management.exception;

public class RequestCannotBeProcessedException extends RuntimeException {
    public RequestCannotBeProcessedException(String message) {
        super(message);
    }
}
