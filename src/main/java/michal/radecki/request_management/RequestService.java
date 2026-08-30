package michal.radecki.request_management;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import michal.radecki.request_management.dto.RequestDto;
import michal.radecki.request_management.dto.RequestStateHistoryDto;
import michal.radecki.request_management.entity.RequestEntity;
import michal.radecki.request_management.entity.RequestStateHistoryEntity;
import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.mapper.RequestMapper;
import michal.radecki.request_management.repository.RequestRepository;
import michal.radecki.request_management.repository.RequestStateHistoryRepository;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.UpdateBodyRequest;
import michal.radecki.request_management.response.RequestPublishResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestStateHistoryRepository requestStateHistoryRepository;
    private final PublicationIdentifierGenerator publicationIdentifierGenerator;

    public RequestService(RequestRepository requestRepository,
                          RequestStateHistoryRepository requestStateHistoryRepository,
                          @Qualifier("myPublicationIdentifierGenerator") PublicationIdentifierGenerator publicationIdentifierGenerator) {
        this.requestRepository = requestRepository;
        this.requestStateHistoryRepository = requestStateHistoryRepository;
        this.publicationIdentifierGenerator = publicationIdentifierGenerator;
    }

    Integer createRequest(@Valid CreateRequest request) {
        RequestEntity entity = RequestMapper.toEntity(request);
        entity = requestRepository.save(entity);
        requestStateHistoryRepository.save(new RequestStateHistoryEntity(entity));
        return entity.getRequestId();
    }

    @Transactional
    void deleteRequest(Integer requestId,
                       String reason) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
           throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.CREATED) {
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be deleted because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED state");
            }
            requestEntity.setState(RequestState.DELETED);
            requestEntity.setReason(reason);
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
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
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be verified because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED state");
            }
            requestEntity.setState(RequestState.VERIFIED);
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
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
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be accepted because it is in " + requestEntity.getState() + " state" +
                        ", not in VERIFIED state");
            }
            requestEntity.setState(RequestState.ACCEPTED);
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
        }
    }

    @Transactional
    void rejectRequest(Integer requestId,
                       String reason) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (!Set.of(RequestState.VERIFIED, RequestState.ACCEPTED).contains(requestEntity.getState())) {
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be rejected because it is in " + requestEntity.getState() + " state" +
                        ", not in VERIFIED or ACCEPTED state");
            }
            requestEntity.setState(RequestState.REJECTED);
            requestEntity.setReason(reason);
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
        }
    }

    @Transactional
    RequestPublishResponse publishRequest(Integer requestId) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (requestEntity.getState() != RequestState.ACCEPTED) {
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be published because it is in " + requestEntity.getState() + " state" +
                        ", not in ACCEPTED state");
            }
            String publicationIdentifier = publicationIdentifierGenerator.generate();
            requestEntity.setPublicationIdentifier(publicationIdentifier);
            requestEntity.setState(RequestState.PUBLISHED);
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
            return new RequestPublishResponse(requestId, publicationIdentifier);
        }
    }

    @Transactional
    void updateBody(Integer requestId,
                    @Valid UpdateBodyRequest request) {
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        if (requestEntityOpt.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        } else {
            RequestEntity requestEntity = requestEntityOpt.get();
            if (!Set.of(RequestState.CREATED, RequestState.VERIFIED).contains(requestEntity.getState())) {
                throw new RequestCannotBeProcessedException("Request with requestId " + requestId +
                        " cannot be updated because it is in " + requestEntity.getState() + " state" +
                        ", not in CREATED or VERIFIED state");
            }
            requestEntity.setBody(request.body());
            requestStateHistoryRepository.save(new RequestStateHistoryEntity(requestEntity));
        }
    }

    Page<RequestDto> getRequestsPage(int pageNumber,
                                     int size,
                                     @Nullable String name,
                                     @Nullable RequestState state) {
        Pageable page = PageRequest.of(pageNumber, size, Sort.by("requestId").ascending());
        Page<RequestEntity> result;
        if (name != null) {
            if (state != null) {
                result = requestRepository.findByNameAndState(name, state, page);
            } else {
                result = requestRepository.findByName(name, page);
            }
        } else if (state != null) {
            result = requestRepository.findByState(state, page);
        } else result = requestRepository.findAll(page);
        if (result == null) return null;
        return result.map(RequestMapper::toDto);
    }

    List<RequestStateHistoryDto> getAuditLog(Integer requestId) {
        List<RequestStateHistoryEntity> states = requestStateHistoryRepository.findAllByRequestId(
                requestId, Sort.by(Sort.Direction.ASC, "changedAt"));
        if (states.isEmpty()) {
            throw new RequestNotFoundException(requestId);
        }
        return states.stream().map(RequestMapper::toDto).collect(Collectors.toList());
    }
}
