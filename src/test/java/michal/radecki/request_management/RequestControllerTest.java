package michal.radecki.request_management;

import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.RequestWithReason;
import michal.radecki.request_management.response.CustomErrorResponse;
import michal.radecki.request_management.response.RequestCreatedResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void when_trying_to_delete_request_in_created_state_then_should_set_state_to_deleted() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        String reason = "request is no longer needed";
        RequestWithReason requestWithReason = new RequestWithReason(reason);
        // when
        MvcResult deleteResult = mockMvc.perform(delete("/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithReason))
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.DELETED);
        assertThat(requestEntity.getReason()).isEqualTo(reason);
    }

    @Test
    void when_trying_to_delete_request_in_different_state_than_created_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        String reason = "request is no longer needed";
        RequestWithReason requestWithReason = new RequestWithReason(reason);
        // when
        MvcResult deleteResult = mockMvc.perform(delete("/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithReason))
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.DELETED);
        assertThat(requestEntity.getReason()).isEqualTo(reason);
        // when
        MvcResult deleteResult2 = mockMvc.perform(delete("/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithReason))
        ).andReturn();
        // then
        assertThat(deleteResult2.getResponse().getStatus()).isEqualTo(400);
        CustomErrorResponse errorResponse = objectMapper.readValue(deleteResult2.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with id " + requestId +
                " cannot be deleted because it is in DELETED state, not in CREATED state");
    }

    @Test
    void when_trying_to_verify_request_in_created_state_then_should_set_state_to_verified() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult deleteResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    void when_trying_to_verify_request_in_different_state_than_created_then_should_return_bad_request_response() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        String reason = "request is no longer needed";
        RequestWithReason requestWithReason = new RequestWithReason(reason);
        // when
        MvcResult deleteResult = mockMvc.perform(delete("/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithReason))
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.DELETED);
        assertThat(requestEntity.getReason()).isEqualTo(reason);
        // when
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(400);
        CustomErrorResponse errorResponse = objectMapper.readValue(verifyResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with id " + requestId +
                " cannot be verified because it is in DELETED state" +
                ", not in CREATED state");
    }

    @Test
    void when_trying_to_accept_request_in_verified_state_then_should_set_state_to_accepted() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult deleteResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        // when
        MvcResult acceptedResult = mockMvc.perform(post("/accept/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(acceptedResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntity2Opt = requestRepository.findById(requestId);
        assertThat(requestEntity2Opt).isPresent();
        RequestEntity requestEntity2 = requestEntity2Opt.get();
        assertThat(requestEntity2.getState()).isEqualTo(RequestState.ACCEPTED);
    }

    @Test
    void when_trying_to_accept_request_in_different_state_than_verified_then_should_not_change_state_and_throw_exception() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult acceptedResult = mockMvc.perform(post("/accept/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(acceptedResult.getResponse().getStatus()).isEqualTo(400);
        Optional<RequestEntity> requestEntity2Opt = requestRepository.findById(requestId);
        assertThat(requestEntity2Opt).isPresent();
        RequestEntity requestEntity2 = requestEntity2Opt.get();
        assertThat(requestEntity2.getState()).isEqualTo(RequestState.CREATED);
        CustomErrorResponse errorResponse = objectMapper.readValue(acceptedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with id " + requestId +
                " cannot be accepted because it is in CREATED state" +
                ", not in VERIFIED state");
    }

    @Test
    void when_trying_to_reject_request_in_verified_state_then_should_set_state_to_rejected() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult deleteResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(deleteResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        // when
        MvcResult acceptedResult = mockMvc.perform(post("/reject/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestWithReason("request is no longer needed")))
        ).andReturn();
        // then
        assertThat(acceptedResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntity2Opt = requestRepository.findById(requestId);
        assertThat(requestEntity2Opt).isPresent();
        RequestEntity requestEntity2 = requestEntity2Opt.get();
        assertThat(requestEntity2.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(requestEntity2.getReason()).isEqualTo("request is no longer needed");
    }

    @Test
    void when_trying_to_reject_request_in_different_state_than_verified_or_accepted_then_should_not_change_state_and_throw_exception() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult acceptedResult = mockMvc.perform(post("/reject/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestWithReason("request is no longer needed")))
        ).andReturn();
        // then
        assertThat(acceptedResult.getResponse().getStatus()).isEqualTo(400);
        Optional<RequestEntity> requestEntity2Opt = requestRepository.findById(requestId);
        assertThat(requestEntity2Opt).isPresent();
        RequestEntity requestEntity2 = requestEntity2Opt.get();
        assertThat(requestEntity2.getState()).isEqualTo(RequestState.CREATED);
        CustomErrorResponse errorResponse = objectMapper.readValue(acceptedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with id " + requestId +
                " cannot be rejected because it is in CREATED state" +
                ", not in VERIFIED or ACCEPTED state");
    }
}
