package michal.radecki.request_management;

import michal.radecki.request_management.dto.RequestDto;
import michal.radecki.request_management.dto.RequestStateHistoryDto;
import michal.radecki.request_management.entity.RequestEntity;
import michal.radecki.request_management.entity.RequestStateHistoryEntity;
import michal.radecki.request_management.repository.RequestRepository;
import michal.radecki.request_management.repository.RequestStateHistoryRepository;
import michal.radecki.request_management.request.CreateRequest;
import michal.radecki.request_management.request.RequestWithReason;
import michal.radecki.request_management.request.UpdateBodyRequest;
import michal.radecki.request_management.response.CustomErrorResponse;
import michal.radecki.request_management.response.PageResponse;
import michal.radecki.request_management.response.RequestCreatedResponse;
import michal.radecki.request_management.response.RequestPublishResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
public class RequestControllerIntegrationTest {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RequestStateHistoryRepository requestStateHistoryRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        requestStateHistoryRepository.deleteAll();
    }

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
        assertThat(requestEntity.getRequestId()).isEqualTo(requestCreatedResponse.id());
        assertThat(requestEntity.getName()).isEqualTo("requestName");
        assertThat(requestEntity.getBody()).isEqualTo("requestBody");
        assertThat(requestEntity.getState()).isEqualTo(RequestState.CREATED);
        assertThat(requestEntity.getReason()).isNull();
        assertThat(requestEntity.getPublicationIdentifier()).isNull();

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(1);
        assert (match.test(requestEntity, requestStateHistoryEntityList.get(0)));
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

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(2);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityDeletedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.DELETED)
                .findFirst();
        assertThat(requestStateHistoryEntityDeletedOpt).isPresent();
        assert (match.test(requestEntity, requestStateHistoryEntityDeletedOpt.get()));
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
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
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
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(2);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityVerifiedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.VERIFIED)
                .findFirst();
        assertThat(requestStateHistoryEntityVerifiedOpt).isPresent();
        assert (match.test(requestEntity, requestStateHistoryEntityVerifiedOpt.get()));
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
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
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
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
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
        Optional<RequestEntity> acceptedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(acceptedRequestEntityOpt).isPresent();
        RequestEntity acceptedRequestEntity = acceptedRequestEntityOpt.get();
        assertThat(acceptedRequestEntity.getState()).isEqualTo(RequestState.ACCEPTED);

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(3);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityVerifiedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.VERIFIED)
                .findFirst();
        assertThat(requestStateHistoryEntityVerifiedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityAcceptedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.ACCEPTED)
                .findFirst();
        assertThat(requestStateHistoryEntityAcceptedOpt).isPresent();
        assert (match.test(acceptedRequestEntity, requestStateHistoryEntityAcceptedOpt.get()));
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
        Optional<RequestEntity> createdRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(createdRequestEntityOpt).isPresent();
        RequestEntity createdRequestEntity = createdRequestEntityOpt.get();
        assertThat(createdRequestEntity.getState()).isEqualTo(RequestState.CREATED);
        CustomErrorResponse errorResponse = objectMapper.readValue(acceptedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
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
        Optional<RequestEntity> verifiedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(verifiedRequestEntityOpt).isPresent();
        RequestEntity verifiedRequestEntity = verifiedRequestEntityOpt.get();
        assertThat(verifiedRequestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        // when
        MvcResult rejectedResult = mockMvc.perform(post("/reject/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestWithReason("request is no longer needed")))
        ).andReturn();
        // then
        assertThat(rejectedResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> rejectedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(rejectedRequestEntityOpt).isPresent();
        RequestEntity rejectedRequestEntity = rejectedRequestEntityOpt.get();
        assertThat(rejectedRequestEntity.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(rejectedRequestEntity.getReason()).isEqualTo("request is no longer needed");

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(3);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityVerifiedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.VERIFIED)
                .findFirst();
        assertThat(requestStateHistoryEntityVerifiedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityRejectedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.REJECTED)
                .findFirst();
        assertThat(requestStateHistoryEntityRejectedOpt).isPresent();
        assert (match.test(rejectedRequestEntity, requestStateHistoryEntityRejectedOpt.get()));
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
        MvcResult rejectedResult = mockMvc.perform(post("/reject/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestWithReason("request is no longer needed")))
        ).andReturn();
        // then
        assertThat(rejectedResult.getResponse().getStatus()).isEqualTo(400);
        Optional<RequestEntity> createdRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(createdRequestEntityOpt).isPresent();
        RequestEntity createdRequestEntity = createdRequestEntityOpt.get();
        assertThat(createdRequestEntity.getState()).isEqualTo(RequestState.CREATED);
        CustomErrorResponse errorResponse = objectMapper.readValue(rejectedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
                " cannot be rejected because it is in CREATED state" +
                ", not in VERIFIED or ACCEPTED state");
    }

    @Test
    void when_trying_to_publish_request_in_accepted_state_then_should_set_state_to_published() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
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
        Optional<RequestEntity> acceptedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(acceptedRequestEntityOpt).isPresent();
        RequestEntity acceptedRequestEntity = acceptedRequestEntityOpt.get();
        assertThat(acceptedRequestEntity.getState()).isEqualTo(RequestState.ACCEPTED);
        // when
        MvcResult publishedResult = mockMvc.perform(post("/publish/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(publishedResult.getResponse().getStatus()).isEqualTo(200);
        RequestPublishResponse response = objectMapper.readValue(publishedResult.getResponse().getContentAsString(), RequestPublishResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.requestId()).isEqualTo(requestId);
        assertThat(response.publicationIdentifier()).isNotNull();
        assertThat(response.publicationIdentifier()).isNotBlank();

        Optional<RequestEntity> publishedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(publishedRequestEntityOpt).isPresent();
        RequestEntity publishedRequestEntity = publishedRequestEntityOpt.get();
        assertThat(publishedRequestEntity.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(publishedRequestEntity.getPublicationIdentifier()).isNotNull();
        assertThat(publishedRequestEntity.getPublicationIdentifier()).isNotBlank();
        assertThat(publishedRequestEntity.getPublicationIdentifier().chars()).allMatch(Character::isDigit);

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(4);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityVerifiedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.VERIFIED)
                .findFirst();
        assertThat(requestStateHistoryEntityVerifiedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityAcceptedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.ACCEPTED)
                .findFirst();
        assertThat(requestStateHistoryEntityAcceptedOpt).isPresent();
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityPublishedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.PUBLISHED)
                .findFirst();
        assertThat(requestStateHistoryEntityPublishedOpt).isPresent();
        assert (match.test(publishedRequestEntity, requestStateHistoryEntityPublishedOpt.get()));
    }

    @Test
    void when_trying_to_publish_request_in_different_state_than_accepted_then_should_not_change_state_and_throw_exception() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult publishedResult = mockMvc.perform(post("/publish/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(publishedResult.getResponse().getStatus()).isEqualTo(400);
        Optional<RequestEntity> createdRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(createdRequestEntityOpt).isPresent();
        RequestEntity createdRequestEntity = createdRequestEntityOpt.get();
        assertThat(createdRequestEntity.getState()).isEqualTo(RequestState.CREATED);
        CustomErrorResponse errorResponse = objectMapper.readValue(publishedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
                " cannot be published because it is in CREATED state" +
                ", not in ACCEPTED state");
    }

    @Test
    void when_trying_to_update_body_in_request_with_created_or_verified_state_then_should_update_body() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        // when
        MvcResult updatedResult = mockMvc.perform(put("/update/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBodyRequest("updatedRequestBody")))
        ).andReturn();
        // then
        assertThat(updatedResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> updatedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(updatedRequestEntityOpt).isPresent();
        RequestEntity updatedRequestEntity = updatedRequestEntityOpt.get();
        assertThat(updatedRequestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        assertThat(updatedRequestEntity.getBody()).isEqualTo("updatedRequestBody");

        List<RequestStateHistoryEntity> requestStateHistoryEntityList = requestStateHistoryRepository.findAllByRequestId(
                requestCreatedResponse.id(), sortByChangedAt());
        assertThat(requestStateHistoryEntityList).isNotNull();
        assertThat(requestStateHistoryEntityList).hasSize(3);
        Optional<RequestStateHistoryEntity> requestStateHistoryEntityCreatedOpt = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.CREATED)
                .findFirst();
        assertThat(requestStateHistoryEntityCreatedOpt).isPresent();
        List<RequestStateHistoryEntity> requestStateHistoryEntityVerifiedList = requestStateHistoryEntityList.stream()
                .filter(r -> r.getState() == RequestState.VERIFIED)
                .collect(Collectors.toList());
        assertThat(requestStateHistoryEntityVerifiedList).isNotNull();
        assertThat(requestStateHistoryEntityVerifiedList).hasSize(2);
        Optional<RequestStateHistoryEntity> updatedRequestOpt = requestStateHistoryEntityVerifiedList.stream()
                .filter(item -> item.getBody().equals("updatedRequestBody"))
                .findFirst();
        assertThat(updatedRequestOpt).isPresent();
        assert (match.test(updatedRequestEntity, updatedRequestOpt.get()));
    }

    @Test
    void when_trying_to_update_body_in_request_using_empty_body_then_should_throw_exception() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.VERIFIED);
        // when
        MvcResult updatedResult = mockMvc.perform(put("/update/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBodyRequest("")))
        ).andReturn();
        // then
        assertThat(updatedResult.getResponse().getStatus()).isEqualTo(400);
        assertThat(updatedResult.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
        // when
        MvcResult updatedResult2 = mockMvc.perform(put("/update/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBodyRequest(null)))
        ).andReturn();
        // then
        assertThat(updatedResult2.getResponse().getStatus()).isEqualTo(400);
        assertThat(updatedResult2.getResponse().getErrorMessage()).isEqualTo("Invalid request content.");
    }

    @Test
    void when_trying_to_update_body_in_request_with_different_state_than_created_or_verified_then_should_not_update_body_and_throw_exception() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult verifyResult = mockMvc.perform(delete("/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RequestWithReason("request is no longer needed")))
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> requestEntityOpt = requestRepository.findById(requestId);
        assertThat(requestEntityOpt).isPresent();
        RequestEntity requestEntity = requestEntityOpt.get();
        assertThat(requestEntity.getState()).isEqualTo(RequestState.DELETED);
        // when
        MvcResult updatedResult = mockMvc.perform(put("/update/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBodyRequest("updatedRequestBody")))
        ).andReturn();
        // then
        assertThat(updatedResult.getResponse().getStatus()).isEqualTo(400);
        CustomErrorResponse errorResponse = objectMapper.readValue(updatedResult.getResponse().getContentAsString(), CustomErrorResponse.class);
        assertThat(errorResponse.message()).isEqualTo("Request with requestId " + requestId +
                " cannot be updated because it is in DELETED state" +
                ", not in CREATED or VERIFIED state");
        Optional<RequestEntity> deletedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(deletedRequestEntityOpt).isPresent();
        RequestEntity deletedRequestEntity = deletedRequestEntityOpt.get();
        assertThat(deletedRequestEntity.getState()).isEqualTo(RequestState.DELETED);
        assertThat(deletedRequestEntity.getBody()).isEqualTo("requestBody");
    }

    @Test
    void when_trying_to_get_first_requests_page_then_should_return_page_of_requests() throws Exception {
        // given
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Integer id = createRequest("name_" + i, "body_" + i);
            requestIds.add(id);
        }
        // when
        MvcResult createResult = mockMvc.perform(get("/browse")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        PageResponse<RequestDto> page = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<PageResponse<RequestDto>>() {
        });
        // then
        assertThat(page).isNotNull();
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(12);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.content().size()).isEqualTo(10);
        Optional<RequestDto> requestDtoOpt = page.content().stream().findFirst();
        assertThat(requestDtoOpt).isPresent();
        RequestDto requestDto = requestDtoOpt.get();
        assertThat(requestDto.name()).isNotNull();
        assertThat(requestDto.name()).startsWith("name_");
        assertThat(requestDto.body()).isNotNull();
        assertThat(requestDto.body()).startsWith("body_");
    }

    @Test
    void when_trying_to_get_second_requests_page_then_should_return_page_of_requests() throws Exception {
        // given
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Integer id = createRequest("name_" + i, "body_" + i);
            requestIds.add(id);
        }
        // when
        MvcResult createResult = mockMvc.perform(get("/browse?page=1")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        PageResponse<RequestDto> page = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<PageResponse<RequestDto>>() {
        });
        // then
        assertThat(page).isNotNull();
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(12);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.content().size()).isEqualTo(2);
        Optional<RequestDto> requestDtoOpt = page.content().stream().findFirst();
        assertThat(requestDtoOpt).isPresent();
        RequestDto requestDto = requestDtoOpt.get();
        assertThat(requestDto.name()).isNotNull();
        assertThat(requestDto.name()).startsWith("name_");
        assertThat(requestDto.body()).isNotNull();
        assertThat(requestDto.body()).startsWith("body_");
    }

    @Test
    void when_trying_to_get_requests_page_with_specific_name_then_should_return_filtered_page_of_requests() throws Exception {
        // given
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Integer id = createRequest("name_" + i, "body_" + i);
            requestIds.add(id);
        }
        // when
        MvcResult createResult = mockMvc.perform(get("/browse?name=name_1")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        PageResponse<RequestDto> page = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<PageResponse<RequestDto>>() {
        });
        // then
        assertThat(page).isNotNull();
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.content().size()).isEqualTo(1);
        Optional<RequestDto> requestDtoOpt = page.content().stream().findFirst();
        assertThat(requestDtoOpt).isPresent();
        RequestDto requestDto = requestDtoOpt.get();
        assertThat(requestDto.name()).isNotNull();
        assertThat(requestDto.name()).isEqualTo("name_1");
        assertThat(requestDto.body()).isNotNull();
        assertThat(requestDto.body()).isEqualTo("body_1");
    }

    @Test
    void when_trying_to_get_requests_page_with_specific_state_then_should_return_filtered_page_of_requests() throws Exception {
        // given
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Integer id = createRequest("name_" + i, "body_" + i);
            requestIds.add(id);
        }
        int counter = 0;
        for (Integer id : requestIds) {
            if (counter % 3 == 0) {
                verifyRequest(id);
            }
            counter++;
        }
        // when
        MvcResult createResult = mockMvc.perform(get("/browse?state=VERIFIED")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        PageResponse<RequestDto> page = objectMapper.readValue(createResult.getResponse().getContentAsString(), new TypeReference<PageResponse<RequestDto>>() {
        });
        // then
        assertThat(page).isNotNull();
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.content().size()).isEqualTo(4);
        Optional<RequestDto> requestDtoOpt = page.content().stream().findFirst();
        assertThat(requestDtoOpt).isPresent();
        RequestDto requestDto = requestDtoOpt.get();
        assertThat(requestDto.name()).isNotNull();
        assertThat(requestDto.name()).startsWith("name_");
        assertThat(requestDto.body()).isNotNull();
        assertThat(requestDto.body()).startsWith("body_");
    }

    @Test
    void when_trying_to_get_audit_then_should_return_record_for_every_request_state_of_specific_request() throws Exception {
        // given
        CreateRequest createRequest = new CreateRequest("requestName", "requestBody");
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        Integer requestId = requestCreatedResponse.id();
        // when
        MvcResult verifyResult = mockMvc.perform(post("/verify/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(verifyResult.getResponse().getStatus()).isEqualTo(200);
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
        Optional<RequestEntity> acceptedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(acceptedRequestEntityOpt).isPresent();
        RequestEntity acceptedRequestEntity = acceptedRequestEntityOpt.get();
        assertThat(acceptedRequestEntity.getState()).isEqualTo(RequestState.ACCEPTED);
        // when
        MvcResult publishedResult = mockMvc.perform(post("/publish/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(publishedResult.getResponse().getStatus()).isEqualTo(200);
        Optional<RequestEntity> publishedRequestEntityOpt = requestRepository.findById(requestId);
        assertThat(publishedRequestEntityOpt).isPresent();
        RequestEntity publishedRequestEntity = publishedRequestEntityOpt.get();
        assertThat(publishedRequestEntity.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(publishedRequestEntity.getPublicationIdentifier()).isNotNull();
        assertThat(publishedRequestEntity.getPublicationIdentifier()).isNotBlank();
        assertThat(publishedRequestEntity.getPublicationIdentifier().chars()).allMatch(Character::isDigit);
        // when
        MvcResult auditlogResult = mockMvc.perform(get("/auditlog/" + requestId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(auditlogResult.getResponse().getStatus()).isEqualTo(200);
        List<RequestStateHistoryDto> auditlogDto = objectMapper.readValue(auditlogResult.getResponse().getContentAsString(), new TypeReference<List<RequestStateHistoryDto>>(){});
        assertThat(auditlogDto).isNotEmpty();
        assertThat(auditlogDto).hasSize(4);

        Optional<RequestStateHistoryDto> createdDtoOpt = auditlogDto.stream().filter(item -> item.state().equals(RequestState.CREATED)).findFirst();
        assertThat(createdDtoOpt).isPresent();
        RequestStateHistoryDto createdDto = createdDtoOpt.get();

        Optional<RequestStateHistoryDto> verifiedDtoOpt = auditlogDto.stream().filter(item -> item.state().equals(RequestState.VERIFIED)).findFirst();
        assertThat(verifiedDtoOpt).isPresent();
        RequestStateHistoryDto verifiedDto = verifiedDtoOpt.get();
        assertThat(verifiedDto.changedAt().isAfter(createdDto.changedAt())).isTrue();

        Optional<RequestStateHistoryDto> acceptedDtoOpt = auditlogDto.stream().filter(item -> item.state().equals(RequestState.ACCEPTED)).findFirst();
        assertThat(acceptedDtoOpt).isPresent();
        RequestStateHistoryDto acceptedDto = acceptedDtoOpt.get();
        assertThat(acceptedDto.changedAt().isAfter(verifiedDto.changedAt())).isTrue();

        Optional<RequestStateHistoryDto> publishedDtoOpt = auditlogDto.stream().filter(item -> item.state().equals(RequestState.PUBLISHED)).findFirst();
        assertThat(publishedDtoOpt).isPresent();
        RequestStateHistoryDto publishedDto = publishedDtoOpt.get();
        assertThat(publishedDto.changedAt().isAfter(acceptedDto.changedAt())).isTrue();
    }

    @Test
    void when_trying_to_get_audit_for_not_existing_request_id_then_should_return_not_found_response() throws Exception {
        // given
        // when
        MvcResult auditlogResult = mockMvc.perform(get("/auditlog/1")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        // then
        assertThat(auditlogResult.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(auditlogResult.getResponse().getContentAsString()).contains("Request with requestId 1 not found");
    }


        private Integer createRequest(String name, String body) throws Exception {
        CreateRequest createRequest = new CreateRequest(name, body);
        MvcResult createResult = mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn();
        RequestCreatedResponse requestCreatedResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), RequestCreatedResponse.class);
        return requestCreatedResponse.id();
    }

    private void verifyRequest(Integer id) throws Exception {
        mockMvc.perform(post("/verify/" + id)
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
    }

    private final BiPredicate<RequestEntity, RequestStateHistoryEntity> match = (requestEntity, requestStateHistoryEntity) ->
            requestEntity.getRequestId().equals(requestStateHistoryEntity.getRequestId()) &&
                    requestEntity.getName().equals(requestStateHistoryEntity.getName()) &&
                    requestEntity.getBody().equals(requestStateHistoryEntity.getBody()) &&
                    (
                            (requestEntity.getReason() == null && requestStateHistoryEntity.getReason() == null) ||
                                    (requestEntity.getReason() != null && requestEntity.getReason().equals(requestStateHistoryEntity.getReason()))
                    ) &&
                    (
                            (requestEntity.getPublicationIdentifier() == null && requestStateHistoryEntity.getPublicationIdentifier() == null) ||
                                    (requestEntity.getPublicationIdentifier() != null && requestEntity.getPublicationIdentifier().equals(requestStateHistoryEntity.getPublicationIdentifier()))
                    ) &&
                    requestEntity.getState().equals(requestStateHistoryEntity.getState());

    private Sort sortByChangedAt() {
        return Sort.by(Sort.Direction.ASC, "changedAt");
    }
}
