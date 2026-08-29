package michal.radecki.request_management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public class RequestControllerTest {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void when_trying_to_create_request_then_should_return_request_id() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        // when
        MvcResult result = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(result.getResponse().getContentAsString(), RequestCreatedResponse.class);
        assertThat(requestCreatedResponse).isNotNull();
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestCreatedResponse.id());
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getId()).isEqualTo(requestCreatedResponse.id());
        assertThat(requestEntity.getName()).isEqualTo("requestName");
        assertThat(requestEntity.getBody()).isEqualTo("requestBody");
        assertThat(requestEntity.getState()).isEqualTo(RequestState.CREATED);
        assertThat(requestEntity.getReason()).isNull();
        assertThat(requestEntity.getPublicationIdentifier()).isNull();
    }

    @Test
    void when_trying_to_create_request_without_name_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest(null, "requestBody");
        // when
        MvcResult result = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
    }

    @Test
    void when_trying_to_create_request_with_empty_name_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("", "requestBody");
        // when
        MvcResult result = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
    }

    @Test
    void when_trying_to_create_request_without_request_body_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", null);
        // when
        MvcResult result = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
    }

    @Test
    void when_trying_to_create_request_with_empty_request_body_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "");
        // when
        MvcResult result = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
    }
}
