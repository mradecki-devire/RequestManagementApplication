package michal.radecki.request_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.RequestWithReason;
import michal.radecki.request_management.response.RequestCreatedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping("create")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestCreatedResponse createRequest(@RequestBody @Valid CreateRequest request) {
        return new RequestCreatedResponse(requestService.createRequest(request));
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRequest(@PathVariable Integer id, @RequestBody @Valid RequestWithReason request) {
        requestService.deleteRequest(id, request.reason());
    }

    @PostMapping("verify/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void verifyRequest(@PathVariable Integer id) {
        requestService.verifyRequest(id);
    }

    @PostMapping("accept/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void acceptRequest(@PathVariable Integer id) {
        requestService.acceptRequest(id);
    }

    @PostMapping("reject/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void rejectRequest(@PathVariable Integer id, @RequestBody @Valid RequestWithReason request) {
        requestService.rejectRequest(id, request.reason());
    }

    @PostMapping("publish/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void publishRequest(@PathVariable Integer id) {
        requestService.publishRequest(id);
    }
}
