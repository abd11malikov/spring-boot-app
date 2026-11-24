package com.epam.gym.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_includesCustomIndicators() throws Exception {
        // given
        String url = "/actuator/health";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.details.count").value(6))
                .andExpect(jsonPath("$.components.connectionPool.status").value("UP"))
                .andExpect(jsonPath("$.components.memory.status").value("UP"));
    }

    @Test
    void prometheus_exposesCustomMetricsWithApplicationAndEnvironmentTags() throws Exception {
        // given
        mockMvc.perform(post("/api/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Metric\",\"lastName\":\"Coach\",\"specializationId\":1}"));

        // when
        ResultActions result = mockMvc.perform(get("/actuator/prometheus"));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "gym_registrations_total{application=\"gym-crm\",environment=\"test\",role=\"trainer\"}")))
                .andExpect(content().string(containsString("gym_authentications_total")))
                .andExpect(content().string(containsString("gym_trainings_search_seconds_count")))
                .andExpect(content().string(containsString("gym_users_active{")));
    }

    @Test
    void info_describesApplication() throws Exception {
        // given
        String url = "/actuator/info";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.app.description").value("Gym CRM REST API"))
                .andExpect(jsonPath("$.java.version").exists());
    }
}
