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
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    
    private static final Integer REQUEST_ID = 127345;

    @BeforeEach
    void init() {
        this.requestService = new RequestService(mockedRequestRepository, mockedRequestStateHistoryRepository, publicationIdentifierGenerator);
        when(mockedRequestStateHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void when_trying_to_create_request_then_should_return_request_id() {
        //given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        RequestEntity requestEntity = new RequestEntity(createRequest.name(), createRequest.body(), RequestState.CREATED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.save(any())).thenReturn(requestEntity);
        //when
        Integer createdRequestId = requestService.createRequest(createRequest);
        //then
        assertThat(createdRequestId).isNotNull();
        assertThat(createdRequestId).isEqualTo(REQUEST_ID);
    }

    @Test
    void when_trying_to_delete_request_in_created_state_then_should_set_state_to_deleted() {
        //given
        String reason = "request is no longer needed";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when
        requestService.deleteRequest(REQUEST_ID, reason);
        //then
        assertThat(requestEntity.getState()).isEqualTo(RequestState.DELETED);
        assertThat(requestEntity.getReason()).isEqualTo(reason);
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
        String reason = "request is no longer needed";
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.deleteRequest(REQUEST_ID, reason));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
                " cannot be deleted because it is in " + state.name() + " state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_delete_not_existing_request_then_should_throw_not_found_exception() {
        //given
        String reason = "request is no longer needed";
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.deleteRequest(REQUEST_ID, reason));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID + " not found");
    }

    @Test
    void when_trying_to_verify_request_in_created_state_then_should_set_state_to_verified() {
        //given
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        // when
        requestService.verifyRequest(REQUEST_ID);
        // then
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        ArgumentCaptor<RequestStateHistoryEntity> captor = ArgumentCaptor.forClass(RequestStateHistoryEntity.class);
        verify(mockedRequestStateHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    void when_trying_to_verify_not_existing_request_then_should_throw_not_found_exception() {
        //given
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.verifyRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID + " not found");
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.verifyRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
                " cannot be verified because it is in " + state.name() + " state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_accept_request_in_verified_state_then_should_set_state_to_accepted() {
        //given
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when
        requestService.acceptRequest(REQUEST_ID);
        //then
        assertThat(requestEntity.getState()).isEqualTo(RequestState.ACCEPTED);
    }

    @Test
    void when_trying_to_accept_not_existing_request_then_should_throw_not_found_exception() {
        //given
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.acceptRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID + " not found");
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.acceptRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
                " cannot be accepted because it is in " + state.name() + " state" +
                ", not in VERIFIED state");
    }

    @Test
    void when_trying_to_reject_request_in_verified_state_then_should_set_state_to_rejected() {
        //given
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when
        requestService.rejectRequest(REQUEST_ID, "request is no longer needed");
        //then
        assertThat(requestEntity.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(requestEntity.getReason()).isEqualTo("request is no longer needed");
    }

    @Test
    void when_trying_to_reject_not_existing_request_then_should_throw_not_found_exception() {
        //given
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.rejectRequest(REQUEST_ID, "request is no longer needed"));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID + " not found");
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.rejectRequest(REQUEST_ID, "request is no longer needed"));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
                " cannot be rejected because it is in " + state.name() + " state" +
                ", not in VERIFIED or ACCEPTED state");
    }

    @Test
    void when_trying_to_publish_request_in_accepted_state_then_should_set_state_to_published() {
        //given
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.ACCEPTED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        when(publicationIdentifierGenerator.generate()).thenReturn("123123");
        //when
        RequestPublishResponse response = requestService.publishRequest(REQUEST_ID);
        //then
        assertThat(requestEntity.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(requestEntity.getPublicationIdentifier()).isEqualTo("123123");
        assertThat(response.publicationIdentifier()).isEqualTo("123123");
    }

    @Test
    void when_trying_to_publish_not_existing_request_then_should_throw_not_found_exception() {
        //given
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.publishRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " + REQUEST_ID + " not found");
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.publishRequest(REQUEST_ID));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when
        requestService.updateBody(REQUEST_ID, new UpdateBodyRequest("new body"));
        //then
        assertThat(requestEntity.getBody()).isEqualTo("new body");
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
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", state);
        requestEntity.setRequestId(REQUEST_ID);
        when(mockedRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.updateBody(REQUEST_ID, new UpdateBodyRequest("newBody")));
        assertThat(exception.getMessage()).isEqualTo("Request with requestId " +REQUEST_ID +
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
        RequestEntity requestEntity = createRequest(1, RequestState.CREATED);
        requestEntity.setRequestId(REQUEST_ID);
        RequestStateHistoryEntity requestStateHistoryForCreatedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.VERIFIED);
        RequestStateHistoryEntity requestStateHistoryForVerifiedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.ACCEPTED);
        RequestStateHistoryEntity requestStateHistoryForAcceptedEntity = new RequestStateHistoryEntity(requestEntity);
        requestEntity.setState(RequestState.PUBLISHED);
        RequestStateHistoryEntity requestStateHistoryForPublishedEntity = new RequestStateHistoryEntity(requestEntity);
        List<RequestStateHistoryEntity> requestStatesHistory = List.of(requestStateHistoryForCreatedEntity, requestStateHistoryForVerifiedEntity,
                requestStateHistoryForAcceptedEntity, requestStateHistoryForPublishedEntity);
        when(mockedRequestStateHistoryRepository.findAllByRequestId(eq(REQUEST_ID), any(Sort.class))).thenReturn(requestStatesHistory);
        // when
        List<RequestStateHistoryDto> history = requestService.getAuditLog(REQUEST_ID);
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
