package com.epam.trainerworkload.web;

import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.web.logging.TransactionIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The REST API through the real application context: security, validation, errors and logging headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrainerWorkloadApiTest {

    private static final String URL = "/api/trainer-workloads";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addThenDelete_updatesMonthlySummary() throws Exception {
        // given
        send(event("Api.Trainer", "2026-11-10", 60, "ADD")).andExpect(status().isOk());
        send(event("Api.Trainer", "2026-11-12", 45, "ADD")).andExpect(status().isOk());
        send(event("Api.Trainer", "2027-01-05", 30, "ADD")).andExpect(status().isOk());

        // when
        send(event("Api.Trainer", "2026-11-10", 60, "DELETE")).andExpect(status().isOk());

        // then
        mockMvc.perform(get(URL + "/Api.Trainer").header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Api.Trainer"))
                .andExpect(jsonPath("$.trainerFirstName").value("Nodira"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(11))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(45))
                .andExpect(jsonPath("$.years[1].year").value(2027));
        mockMvc.perform(get(URL + "/Api.Trainer/years/2026/months/11")
                        .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(45));
    }

    @Test
    void getWorkload_unknownTrainer_returns404() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(URL + "/Nobody.Here")
                .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No workload for trainer Nobody.Here"));
    }

    @Test
    void invalidRequest_returns400WithViolations() throws Exception {
        // when
        ResultActions result = send("{\"trainerUsername\":\"\",\"trainingDuration\":-5}");

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[?(@.field == 'actionType')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'trainingDuration')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'trainerUsername')]").exists());
    }

    @Test
    void unknownActionType_returns400() throws Exception {
        // when
        ResultActions result = send(event("Api.Trainer", "2026-11-10", 60, "CANCEL"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    @Test
    void invalidMonth_returns400() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(URL + "/Api.Trainer/years/2026/months/13")
                .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("month"));
    }

    @Test
    void nonNumericYear_returns400() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(URL + "/Api.Trainer/years/abc/months/1")
                .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void missingToken_returns401WithBearerChallenge() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(event("Api.Trainer", "2026-11-10", 60, "ADD")));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.message").value("Missing service token"));
    }

    @Test
    void tokensThatFailChecks_areRejected() throws Exception {
        String[] badTokens = {
                ServiceTokens.bearer(ServiceTokens.OTHER_SECRET, "gym-crm", "trainer-workload-service",
                        Duration.ofMinutes(5)),
                ServiceTokens.bearer(ServiceTokens.SECRET, "someone-else", "trainer-workload-service",
                        Duration.ofMinutes(5)),
                ServiceTokens.bearer(ServiceTokens.SECRET, "gym-crm", "another-service", Duration.ofMinutes(5)),
                ServiceTokens.bearer(ServiceTokens.SECRET, "gym-crm", "trainer-workload-service",
                        Duration.ofMinutes(-5)),
                "Bearer not-a-jwt"
        };
        for (String token : badTokens) {
            mockMvc.perform(get(URL + "/Api.Trainer").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid service token"));
        }
    }

    @Test
    void transactionIdFromCaller_isKeptInResponseAndErrors() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(URL + "/Nobody.Here")
                .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid())
                .header(TransactionIdFilter.HEADER, "gym-crm-tx-1"));

        // then
        result.andExpect(header().string(TransactionIdFilter.HEADER, "gym-crm-tx-1"))
                .andExpect(jsonPath("$.transactionId").value("gym-crm-tx-1"));
    }

    @Test
    void wrongMethod_returns405() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()));

        // then
        result.andExpect(status().isMethodNotAllowed());
    }

    @Test
    void wrongContentType_returns415() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid())
                .contentType(MediaType.TEXT_PLAIN).content("hello"));

        // then
        result.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unknownUrl_returns404() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/unknown").header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    void healthAndApiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Trainer Workload REST API"))
                .andExpect(jsonPath("$.components.securitySchemes.serviceToken.scheme").value("bearer"));
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post(URL)
                .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String event(String username, String date, int duration, String action) {
        return "{\"trainerUsername\":\"" + username + "\",\"trainerFirstName\":\"Nodira\","
                + "\"trainerLastName\":\"Karimova\",\"isActive\":true,\"trainingDate\":\"" + date + "\","
                + "\"trainingDuration\":" + duration + ",\"actionType\":\"" + action + "\"}";
    }
}
