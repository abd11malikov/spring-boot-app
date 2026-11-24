package com.epam.gym.web.config;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTrainingTypes_throughRealContexts_returnsSeededTypes() throws Exception {
        // given
        String authorization = registerTraineeAndGetAuthorization();

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, authorization));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].trainingType").value("Cardio"))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void getTrainingTypes_withoutCredentials_returns401JsonWithChallenge() throws Exception {
        // given
        String url = "/api/training-types";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"gym-crm\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Missing bearer token"));
    }

    @Test
    void errorResponse_carriesSameTransactionIdAsResponseHeader() throws Exception {
        // given
        String incomingId = "client-trace-7";

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(TransactionIdFilter.HEADER, incomingId));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(TransactionIdFilter.HEADER, incomingId))
                .andExpect(jsonPath("$.transactionId").value(incomingId));
    }

    @Test
    void unknownUrl_authenticated_returns404Json() throws Exception {
        // given
        String url = "/api/does-not-exist";
        String authorization = registerTraineeAndGetAuthorization();

        // when
        ResultActions result = mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, authorization));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint GET /api/does-not-exist"));
    }

    @Test
    void apiDocs_areServedAsOpenApiJson() throws Exception {
        // given
        String url = "/v3/api-docs";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Gym CRM REST API"))
                .andExpect(jsonPath("$.paths['/api/training-types']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].get.security[0].basicAuth").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists());
    }

    @Test
    void apiDocs_documentErrorResponsesWithErrorSchema() throws Exception {
        // given
        String url = "/v3/api-docs";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(jsonPath("$.paths['/api/trainees'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/trainees'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/RegistrationResponse"))
                .andExpect(jsonPath("$.paths['/api/training-types'].get.responses['500']").exists());
    }

    @Test
    void swaggerUi_isServed() throws Exception {
        // given
        String url = "/swagger-ui/index.html";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    void jsonDates_areWrittenAsIsoStrings() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 3, 10);

        // when
        String json = objectMapper.writeValueAsString(date);

        // then
        assertEquals("\"2026-03-10\"", json);
    }

    private String registerTraineeAndGetAuthorization() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Web\",\"lastName\":\"Tester\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        var response = objectMapper.readTree(registration.getResponse().getContentAsString());
        return "Bearer " + response.get("token").asText();
    }
}
