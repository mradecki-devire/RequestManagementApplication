package michal.radecki.request_management;

import michal.radecki.request_management.exception.RequestCannotBeProcessedException;
import michal.radecki.request_management.exception.RequestNotFoundException;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.UpdateBodyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestServiceTest {

    private RequestService requestService;
    @Mock
    private RequestRepository mockedRequestRepository;
    @Mock
    private PublicationIdentifierGenerator publicationIdentifierGenerator;

    @BeforeEach
    void init() {
        this.requestService = new RequestService(mockedRequestRepository, publicationIdentifierGenerator);
    }

    @Test
    void when_trying_to_create_request_then_should_return_request_id() {
        //given
        Integer id = 127345;
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        RequestEntity requestEntity = new RequestEntity(createRequest.name(), createRequest.body(), RequestState.CREATED);
        requestEntity.setId(id);
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
        requestEntity.setId(id);
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.deleteRequest(id, reason));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
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
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id + " not found");
    }

    @Test
    void when_trying_to_verify_request_in_created_state_then_should_set_state_to_verified() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.CREATED);
        requestEntity.setId(id);
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
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id + " not found");
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.verifyRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
                " cannot be verified because it is in " + state.name() + " state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_accept_request_in_verified_state_then_should_set_state_to_accepted() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setId(id);
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
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id + " not found");
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.acceptRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
                " cannot be accepted because it is in " + state.name() + " state" +
                ", not in VERIFIED state");
    }

    @Test
    void when_trying_to_reject_request_in_verified_state_then_should_set_state_to_rejected() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.VERIFIED);
        requestEntity.setId(id);
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
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id + " not found");
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.rejectRequest(id, "request is no longer needed"));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
                " cannot be rejected because it is in " + state.name() + " state" +
                ", not in VERIFIED or ACCEPTED state");
    }

    @Test
    void when_trying_to_publish_request_in_accepted_state_then_should_set_state_to_published() {
        //given
        Integer id = 127345;
        RequestEntity requestEntity = new RequestEntity("requestName", "requestBody", RequestState.ACCEPTED);
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        when(publicationIdentifierGenerator.generate()).thenReturn("123123");
        //when //then
        assertDoesNotThrow(() -> requestService.publishRequest(id));
    }

    @Test
    void when_trying_to_publish_not_existing_request_then_should_throw_not_found_exception() {
        //given
        Integer id = 127345;
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.empty());
        //when //then
        RequestNotFoundException exception = assertThrows(RequestNotFoundException.class,
                () -> requestService.publishRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id + " not found");
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.publishRequest(id));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
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
        requestEntity.setId(id);
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
        requestEntity.setId(id);
        when(mockedRequestRepository.findById(id)).thenReturn(Optional.of(requestEntity));
        //when //then
        RequestCannotBeProcessedException exception = assertThrows(RequestCannotBeProcessedException.class,
                () -> requestService.updateBody(id, new UpdateBodyRequest("newBody")));
        assertThat(exception.getMessage()).isEqualTo("Request with id " + id +
                " cannot be updated because it is in " + state.name() + " state" +
                ", not in CREATED or VERIFIED state");
    }
}
