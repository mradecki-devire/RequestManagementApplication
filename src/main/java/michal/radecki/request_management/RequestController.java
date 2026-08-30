package michal.radecki.request_management;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.validation.annotation.Validated;
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
@Validated
public class RequestController {

    private final RequestService requestService;

    @Operation(
            summary = "Create request",
            description = "Creates a new request with CREATED state. Both name and body fields are required and can not be empty."
    )
    @PostMapping("create")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestCreatedResponse createRequest(@RequestBody @Valid CreateRequest request) {
        return new RequestCreatedResponse(requestService.createRequest(request));
    }

    @Operation(
            summary = "Delete request",
            description = "Deletes a request with the given request id. A deletion reason is required. " +
                    "Request can be deleted only when it is in CREATED state."
    )
    @DeleteMapping("{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRequest(@PathVariable Integer requestId, @RequestBody @Valid RequestWithReason request) {
        requestService.deleteRequest(requestId, request.reason());
    }

    @Operation(
            summary = "Verify request",
            description = "Changes the state of the request with the given id to VERIFIED. " +
                    "Request can be verified only when it is in CREATED state."
    )
    @PostMapping("verify/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void verifyRequest(@PathVariable Integer requestId) {
        requestService.verifyRequest(requestId);
    }

    @Operation(
            summary = "Accept request",
            description = "Changes the state of the request with the given request id to ACCEPTED. " +
                    "Request can be accepted only when it is in VERIFIED state."
    )
    @PostMapping("accept/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void acceptRequest(@PathVariable Integer requestId) {
        requestService.acceptRequest(requestId);
    }

    @Operation(
            summary = "Reject request",
            description = "Changes the state of the request with the given id to REJECTED. A rejection reason is required. " +
                    "Request can be rejected only when it is in VERIFIED or ACCEPTED state."
    )
    @PostMapping("reject/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void rejectRequest(@PathVariable Integer requestId, @RequestBody @Valid RequestWithReason request) {
        requestService.rejectRequest(requestId, request.reason());
    }

    @Operation(
            summary = "Publish request",
            description = "Changes the state of the request with the given id to PUBLISHED and assigns a unique numeric publication identifier. " +
                    "Request can be published only when it is in ACCEPTED state."
    )
    @PostMapping("publish/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public RequestPublishResponse publishRequest(@PathVariable Integer requestId) {
        return requestService.publishRequest(requestId);
    }

    @Operation(
            summary = "Update request body",
            description = "Updates the body of the request with the given id. The body can only be modified when the request is in CREATED or VERIFIED state"
    )
    @PutMapping("update/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateBody(@PathVariable Integer requestId, @RequestBody @Valid UpdateBodyRequest request) {
        requestService.updateBody(requestId, request);
    }

    @Operation(
            summary = "Browse requests",
            description = "Returns a paginated list of requests, optionally filtered by name or state"
    )
    @GetMapping("browse")
    @ResponseStatus(HttpStatus.OK)
    public Page<RequestDto> browseRequests(
            @Parameter(schema = @Schema(defaultValue = "0"))
            @Min(0)
            @RequestParam(required = false, defaultValue = "0")
            Integer page,

            @Parameter(schema = @Schema(defaultValue = "10"))
            @Min(1)
            @RequestParam(required = false, defaultValue = "10")
            Integer size,

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            RequestState state) {
        return requestService.getRequestsPage(page, size, name, state);
    }

    @Operation(
            summary = "Get request audit log",
            description = "Returns the complete audit history of relevant request changes for the given request id"
    )
    @GetMapping("auditlog/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public List<RequestStateHistoryDto> getAuditLog(@PathVariable Integer requestId) {
        return requestService.getAuditLog(requestId);
    }
}
