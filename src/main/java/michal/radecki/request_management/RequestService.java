package michal.radecki.request_management;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.request.CreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;

    Integer createRequest(@Valid CreateRequest request) {
        RequestEntity entity = new RequestEntity(request.name(), request.body(), RequestState.CREATED);
        entity = requestRepository.save(entity);
        return entity.getId();
    }

    @Transactional
    void deleteRequest(Integer requestId, String reason) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
           throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.CREATED) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be deleted because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED state");
            }
            requestEntity.setState(RequestState.DELETED);
            requestEntity.setReason(reason);
        }
    }

    @Transactional
    void verifyRequest(Integer requestId) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.CREATED) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be verified because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED state");
            }
            requestEntity.setState(RequestState.VERIFIED);
        }
    }
}
