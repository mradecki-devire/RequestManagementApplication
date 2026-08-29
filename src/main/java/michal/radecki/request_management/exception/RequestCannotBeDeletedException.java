package michal.radecki.request_management.exception;

public class RequestCannotBeDeletedException extends RuntimeException {
    public RequestCannotBeDeletedException(String message) {
        super(message);
    }
}
