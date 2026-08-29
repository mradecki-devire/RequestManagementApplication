package michal.radecki.request_management;

import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.response.CustomErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(RequestCannotBeProcessedException.class)
    public ResponseEntity<CustomErrorResponse> handleRequestCannotBeProcessed(
            RequestCannotBeProcessedException exception) {

        CustomErrorResponse error = new CustomErrorResponse(exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<CustomErrorResponse> handleRequestNotFoundException(
            RequestNotFoundException exception) {

        CustomErrorResponse error = new CustomErrorResponse(exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}
