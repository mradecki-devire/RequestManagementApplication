package michal.radecki.request_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestServiceTest {

    private RequestService requestService;
    @Mock
    private RequestRepository mockedRequestRepository;

    @BeforeEach
    void init() {
        this.requestService = new RequestService(mockedRequestRepository);
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
}
