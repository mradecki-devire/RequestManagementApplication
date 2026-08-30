package michal.radecki.request_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import michal.radecki.request_management.dto.RequestDto;
import michal.radecki.request_management.dto.RequestStateHistoryDto;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.RequestWithReason;
import michal.radecki.request_management.request.UpdateBodyRequest;
import michal.radecki.request_management.response.RequestCreatedResponse;
import michal.radecki.request_management.response.RequestPublishResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping("create")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestCreatedResponse createRequest(@RequestBody @Valid CreateRequest request) {
        return new RequestCreatedResponse(requestService.createRequest(request));
    }

    @DeleteMapping("{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRequest(@PathVariable Integer requestId, @RequestBody @Valid RequestWithReason request) {
        requestService.deleteRequest(requestId, request.reason());
    }

    @PostMapping("verify/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void verifyRequest(@PathVariable Integer requestId) {
        requestService.verifyRequest(requestId);
    }

    @PostMapping("accept/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void acceptRequest(@PathVariable Integer requestId) {
        requestService.acceptRequest(requestId);
    }

    @PostMapping("reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void rejectRequest(@PathVariable Integer requestId, @RequestBody @Valid RequestWithReason request) {
        requestService.rejectRequest(requestId, request.reason());
    }

    @PostMapping("publish/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public RequestPublishResponse publishRequest(@PathVariable Integer requestId) {
        return requestService.publishRequest(requestId);
    }

    @PutMapping("update/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateBody(@PathVariable Integer requestId, @RequestBody @Valid UpdateBodyRequest request) {
        requestService.updateBody(requestId, request);
    }

    @GetMapping("browse")
    @ResponseStatus(HttpStatus.OK)
    public Page<RequestDto> browseRequests(@RequestParam(required = false, defaultValue = "0") Integer page,
                                           @RequestParam(required = false, defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) RequestState state) {
        return requestService.getRequestsPage(page, size, name, state);
    }

    @GetMapping("auditlog/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public List<RequestStateHistoryDto> getAuditLog(@PathVariable Integer requestId) {
        return requestService.getAuditLog(requestId);
    }
}
