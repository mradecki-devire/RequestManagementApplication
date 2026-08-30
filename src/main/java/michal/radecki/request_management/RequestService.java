package michal.radecki.request_management;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.UpdateBodyRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;

@Service
@Validated
public class RequestService {

    private final RequestRepository requestRepository;
    private final PublicationIdentifierGenerator publicationIdentifierGenerator;

    public RequestService(RequestRepository requestRepository,
                          @Qualifier("myPublicationIdentifierGenerator") PublicationIdentifierGenerator publicationIdentifierGenerator) {
        this.requestRepository = requestRepository;
        this.publicationIdentifierGenerator = publicationIdentifierGenerator;
    }

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

    @Transactional
    void acceptRequest(Integer requestId) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.VERIFIED) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be accepted because it is in " + requestEntity.getState() + " state" +
                        ", not in VERIFIED state");
            }
            requestEntity.setState(RequestState.ACCEPTED);
        }
    }

    @Transactional
    void rejectRequest(Integer requestId, String reason) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (!Set.of(RequestState.VERIFIED, RequestState.ACCEPTED).contains(requestEntity.getState())) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be rejected because it is in " + requestEntity.getState() + " state" +
                        ", not in VERIFIED or ACCEPTED state");
            }
            requestEntity.setState(RequestState.REJECTED);
            requestEntity.setReason(reason);
        }
    }

    @Transactional
    void publishRequest(Integer requestId) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.ACCEPTED) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be published because it is in " + requestEntity.getState() + " state" +
                        ", not in ACCEPTED state");
            }
            requestEntity.setState(RequestState.PUBLISHED);
            String publicationIdentifier = publicationIdentifierGenerator.generate();
            requestEntity.setPublicationIdentifier(publicationIdentifier);
        }
    }

    @Transactional
    void updateBody(Integer requestId, @Valid UpdateBodyRequest request) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (!Set.of(RequestState.CREATED, RequestState.VERIFIED).contains(requestEntity.getState())) {
                throw new RequestCannotBeProcessedException("Request with id " + requestId +
                        " cannot be updated because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED or VERIFIED state");
            }
            requestEntity.setBody(request.body());
        }
    }
}
