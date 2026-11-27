package com.epam.trainerworkload.web;

import com.epam.trainerworkload.MongoTestContainer;
import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.service.TrainerWorkloadUpdate;
import com.epam.trainerworkload.web.logging.TransactionIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;

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
@Import(MongoTestContainer.class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrainerWorkloadApiTest {

    private static final String URL = "/api/trainer-workloads";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainerWorkloadService service;

    @Test
    void getWorkload_returnsSummaryByYearAndMonth() throws Exception {
        // given
        apply("2026-11-10", 60, ActionType.ADD);
        apply("2026-11-12", 45, ActionType.ADD);
        apply("2027-01-05", 30, ActionType.ADD);
        apply("2026-11-10", 60, ActionType.DELETE);

        // when / then
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
    void findByName_returnsTrainersWithThatName() throws Exception {
        // given
        service.apply(new TrainerWorkloadUpdate("Named.Trainer", "Lola", "Rahimova", true,
                LocalDate.of(2026, 11, 10), 30, ActionType.ADD));

        // when / then
        mockMvc.perform(get(URL).param("firstName", "Lola").param("lastName", "Rahimova")
                        .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainerUsername").value("Named.Trainer"))
                .andExpect(jsonPath("$[0].years[0].months[0].trainingSummaryDuration").value(30));
        mockMvc.perform(get(URL).param("firstName", "Nobody").param("lastName", "Here")
                        .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findByName_missingOrBlankName_returns400() throws Exception {
        mockMvc.perform(get(URL).param("firstName", "Lola").header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'lastName' is required"));
        mockMvc.perform(get(URL).param("firstName", "Lola").param("lastName", " ")
                        .header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("lastName"));
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
        ResultActions result = mockMvc.perform(get(URL + "/Api.Trainer"));

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
    void updatesOverRest_areNotAccepted() throws Exception {
        // when: workload changes arrive only as messages
        ResultActions result = mockMvc.perform(post(URL + "/Api.Trainer").header(HttpHeaders.AUTHORIZATION, ServiceTokens.valid())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        // then
        result.andExpect(status().isMethodNotAllowed());
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


    private void apply(String date, int minutes, ActionType action) {
        service.apply(new TrainerWorkloadUpdate("Api.Trainer", "Nodira", "Karimova", true, LocalDate.parse(date),
                minutes, action));
    }
}
