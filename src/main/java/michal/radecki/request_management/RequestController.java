package michal.radecki.request_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
}
