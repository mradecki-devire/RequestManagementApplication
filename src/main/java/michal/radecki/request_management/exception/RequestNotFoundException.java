package michal.radecki.request_management.exception;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Integer id) {
        super("Request with requestId " + id + " not found");
    }
}
