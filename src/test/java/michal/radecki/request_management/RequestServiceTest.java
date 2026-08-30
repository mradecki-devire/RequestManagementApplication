package michal.radecki.request_management;

import michal.radecki.request_management.dto.RequestDto;
import michal.radecki.request_management.dto.RequestStateHistoryDto;
import michal.radecki.request_management.entity.RequestEntity;
import michal.radecki.request_management.entity.RequestStateHistoryEntity;
import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.repository.RequestRepository;
import michal.radecki.request_management.repository.RequestStateHistoryRepository;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.UpdateBodyRequest;
import michal.radecki.request_management.response.RequestPublishResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequestServiceTest {

    private RequestService requestService;
    @Mock
    private RequestRepository mockedRequestRepository;
    @Mock
    private RequestStateHistoryRepository mockedRequestStateHistoryRepository;
    @Mock
    private PublicationIdentifierGenerator publicationIdentifierGenerator;

    @BeforeEach
    void init() {
        this.requestService = new RequestService(mockedRequestRepository, mockedRequestStateHistoryRepository, publicationIdentifierGenerator);
        when(mockedRequestStateHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void when_trying_to_create_request_then_should_return_request_id() {
        //given
        Integer id = 127345;
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        RequestEntity requestEntity = new RequestEntity(createRequest.name(), createRequest.body(), RequestState.CREATED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.save(any())).thenReturn(requestEntity);
        //when
        Integer createdRequestId = requestService.createRequest(createRequest);
        //then
        assertThat(createdRequestId).isNotNull();
        assertThat(createdRequestId).isEqualTo(id);
    }

    @Test
    void when_trying_to_delete_request_in_created_state_then_should_set_state_to_deleted() {
        //given
        Integer id = 127345;
        String reason = "request is no longer needed";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        assertDoesNotThrow(() -> requestService.deleteRequest(id, reason));
    }

    @ParameterizedTest
    @CsvSource({
            "PUBLISHED",
            "DELETED",
            "VERIFIED",
            "REJECTED",
            "ACCEPTED"
    })
    void when_trying_to_delete_request_in_state_different_than_created_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        String reason = "request is no longer needed";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.deleteRequest(id, reason));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be deleted because it is in " + state.name() + " state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_delete_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        String reason = "request is no longer needed";
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.deleteRequest(id, reason));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id + " not found");
    }

    @Test
    void when_trying_to_verify_request_in_created_state_then_should_set_state_to_verified() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        assertDoesNotThrow(() -> requestService.verifyRequest(id));
    }

    @Test
    void when_trying_to_verify_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.verifyRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id + " not found");
    }

    @ParameterizedTest
    @CsvSource({
            "PUBLISHED",
            "DELETED",
            "VERIFIED",
            "REJECTED",
            "ACCEPTED"
    })
    void when_trying_to_verify_request_in_state_different_than_created_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        String reason = "request is no longer needed";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.verifyRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be verified because it is in " + state.name() + " state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_accept_request_in_verified_state_then_should_set_state_to_accepted() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        assertDoesNotThrow(() -> requestService.acceptRequest(id));
    }

    @Test
    void when_trying_to_accept_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.acceptRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id + " not found");
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED",
            "PUBLISHED",
            "DELETED",
            "REJECTED",
            "ACCEPTED"
    })
    void when_trying_to_accept_request_in_state_different_than_verified_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.acceptRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be accepted because it is in " + state.name() + " state" +
                ", not in VERIFIED state");
    }

    @Test
    void when_trying_to_reject_request_in_verified_state_then_should_set_state_to_rejected() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        assertDoesNotThrow(() -> requestService.rejectRequest(id, "request is no longer needed"));
    }

    @Test
    void when_trying_to_reject_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.rejectRequest(id, "request is no longer needed"));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id + " not found");
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED",
            "PUBLISHED",
            "DELETED",
            "REJECTED"
    })
    void when_trying_to_reject_request_in_state_different_than_verified_or_accepted_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.rejectRequest(id, "request is no longer needed"));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be rejected because it is in " + state.name() + " state" +
                ", not in VERIFIED or ACCEPTED state");
    }

    @Test
    void when_trying_to_publish_request_in_accepted_state_then_should_set_state_to_published() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.ACCEPTED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        when(publicationIdentifierGenerator.generate()).thenReturn("123123");
        //when //then
        RequestPublishResponse publishResponse = assertDoesNotThrow(() -> requestService.publishRequest(id));
        assertThat(publishResponse).isNotNull();
        assertThat(publishResponse.requestId()).isEqualTo(id);
        assertThat(publishResponse.publicationIdentifier()).isNotNull();
    }

    @Test
    void when_trying_to_publish_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.publishRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id + " not found");
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED",
            "PUBLISHED",
            "DELETED",
            "REJECTED",
            "VERIFIED"
    })
    void when_trying_to_publish_request_in_state_different_than_accepted_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.publishRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be published because it is in " + state.name() + " state" +
                ", not in ACCEPTED state");
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED",
            "VERIFIED"
    })
    void when_trying_to_update_body_in_request_with_created_or_verified_state_then_should_set_body() {
        //given
        Integer id = 127345;
        String newBody = "new body";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        assertDoesNotThrow(() -> requestService.updateBody(id, new UpdateBodyRequest(newBody)));
    }

    @ParameterizedTest
    @CsvSource({
            "PUBLISHED",
            "DELETED",
            "REJECTED",
            "ACCEPTED"
    })
    void when_trying_to_update_body_in_request_with_state_different_than_created_of_verified_then_should_throw_exception(RequestState state) {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.updateBody(id, new UpdateBodyRequest("newBody")));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + id +
                " cannot be updated because it is in " + state.name() + " state" +
                ", not in CREATED or VERIFIED state");
    }

    @Test
    void when_trying_to_get_requests_page_then_should_return_empty_page_with_requests() {
        // given
        String name = null;
        RequestState state = null;
        int pageNumber = 0, size = 10;
        when(mockedRequestRepository.findAll((Pageable) any())).thenReturn(Page.empty());
        // when
        Page<RequestDto> requestsPage = requestService.getRequestsPage(pageNumber, size, name, state);
        // then
        assertThat(requestsPage.getTotalElements()).isEqualTo(0);
        assertThat(requestsPage.getTotalPages()).isEqualTo(1);
    }

    @Test
    void when_trying_to_get_requests_page_then_should_return_not_empty_page_with_requests() {
        // given
        String name = null;
        RequestState state = null;
        int pageNumber = 0, size = 10;
        List<RequestEntity> requests = createRequests(7, RequestState.CREATED);
        Page<RequestEntity> page = new PageImpl<>(requests);
        when(mockedRequestRepository.findAll((Pageable) any())).thenReturn(page);
        // when
        Page<RequestDto> requestsPage = requestService.getRequestsPage(pageNumber, size, name, state);
        // then
        assertThat(requestsPage.getTotalElements()).isEqualTo(7);
        assertThat(requestsPage.getTotalPages()).isEqualTo(1);
    }

    @Test
    void when_trying_to_audit_log_then_should_return_request_log_for_id() {
        // given
        Integer requestId = 1;
        RequestEntity requestEntity = createRequest(1, RequestState.CREATED);
        requestEntity.setRequestId(requestId);
        RequestStateHistoryEntity requestStateHistoryForCreatedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.VERIFIED);
        RequestStateHistoryEntity requestStateHistoryForVerifiedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.ACCEPTED);
        RequestStateHistoryEntity requestStateHistoryForAcceptedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.PUBLISHED);
        RequestStateHistoryEntity requestStateHistoryForPublishedEntity = new RequestStateHistoryEntity(requestEntity);
        List<RequestStateHistoryEntity> requestStatesHistory = List.of(requestStateHistoryForCreatedEntity, requestStateHistoryForVerifiedEntity,
                requestStateHistoryForAcceptedEntity, requestStateHistoryForPublishedEntity);
        when(mockedRequestStateHistoryRepository.findAllByRequestId(eq(requestId), any(Sort.class))).thenReturn(requestStatesHistory);
        // when
        List<RequestStateHistoryDto> history = requestService.getAuditLog(requestId);
        // then
        assertThat(history).isNotNull();
        assertThat(history).isNotEmpty();
        assertThat(history).hasSize(4);
        assertThat(history.get(0).state()).isEqualTo(RequestState.CREATED);
        assertThat(history.get(1).state()).isEqualTo(RequestState.VERIFIED);
        assertThat(history.get(2).state()).isEqualTo(RequestState.ACCEPTED);
        assertThat(history.get(3).state()).isEqualTo(RequestState.PUBLISHED);
    }

    private List<RequestEntity> createRequests(int amount, RequestState state) {
        List<RequestEntity> requestEntities = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            requestEntities.add(createRequest(i, state));
        }
        return requestEntities;
    }

    private RequestEntity createRequest(int i, RequestState state) {
        return new RequestEntity("name_" + i, "body_" + i, state);
    }
}
