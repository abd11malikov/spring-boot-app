package com.epam.gym.web.error;

import com.epam.gym.exception.AccessDeniedException;
import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.exception.WorkloadServiceUnavailableException;
import com.epam.gym.TestObjectMapper;
import com.epam.gym.web.logging.TransactionIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(TestObjectMapper.create()))
                .build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void errorResponse_includesCurrentTransactionId() throws Exception {
        // given
        MDC.put(TransactionIdFilter.MDC_KEY, "tx-123");

        // when
        ResultActions result = mockMvc.perform(get("/test/not-found"));

        // then
        result.andExpect(jsonPath("$.transactionId").value("tx-123"));
    }

    @Test
    void validationFailure_returns400WithSortedFieldViolations() throws Exception {
        // given
        String body = "{\"name\":\" \",\"nickname\":\"\"}";

        // when
        ResultActions result = mockMvc.perform(post("/test/validated")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/validated"))
                .andExpect(jsonPath("$.violations[0].field").value("name"))
                .andExpect(jsonPath("$.violations[1].field").value("nickname"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        // given
        String body = "{\"name\":";

        // when
        ResultActions result = mockMvc.perform(post("/test/validated")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                .andExpect(jsonPath("$.violations").doesNotExist());
    }

    @Test
    void invalidValueInBody_namesTheField() throws Exception {
        // given
        String body = "{\"name\":\"A\",\"nickname\":\"B\",\"birthday\":\"2026-13-45\"}";

        // when
        ResultActions result = mockMvc.perform(post("/test/validated")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'birthday' has invalid value '2026-13-45'"));
    }

    @Test
    void invalidQueryParameter_returns400NamingTheParameter() throws Exception {
        // given
        String url = "/test/dated?from=yesterday";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'from' has invalid value 'yesterday'"));
    }

    @Test
    void illegalArgument_returns400WithMessage() throws Exception {
        // given
        String url = "/test/illegal-argument";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("First name is required"));
    }

    @Test
    void authenticationFailure_returns401WithBearerChallenge() throws Exception {
        // given
        String url = "/test/unauthenticated";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"gym-crm\""))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void workloadServiceUnavailable_returns503WithRetryAfterAndNoInternalDetails() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/test/workload-down"));

        // then
        result.andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.message")
                        .value("Trainer workload updates are temporarily unavailable, please try again later"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("connection refused"))));
    }

    @Test
    void accessDenied_returns403() throws Exception {
        // given
        String url = "/test/forbidden";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own profile"));
    }

    @Test
    void notFound_returns404() throws Exception {
        // given
        String url = "/test/not-found";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Trainee X not found"));
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        // given
        String url = "/test/not-found";

        // when
        ResultActions result = mockMvc.perform(delete(url));

        // then
        result.andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void wrongContentType_returns415() throws Exception {
        // given
        String body = "plain text";

        // when
        ResultActions result = mockMvc.perform(post("/test/validated")
                .contentType(MediaType.TEXT_PLAIN).content(body));

        // then
        result.andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void illegalState_returns409() throws Exception {
        // given
        String url = "/test/conflict";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trainee X is already active"));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        // given
        String url = "/test/crash";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"))
                .andExpect(content().string(not(containsString("database password leaked"))));
    }

    @RestController
    static class ThrowingController {

        record Validated(@NotBlank String name, @NotBlank String nickname, LocalDate birthday) {
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody Validated body) {
        }

        @GetMapping("/test/dated")
        void dated(@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        }

        @GetMapping("/test/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("First name is required");
        }

        @GetMapping("/test/unauthenticated")
        void unauthenticated() {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("You can only access your own profile");
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NoSuchElementException("Trainee X not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new IllegalStateException("Trainee X is already active");
        }

        @GetMapping("/test/workload-down")
        void workloadDown() {
            throw new WorkloadServiceUnavailableException(new java.net.ConnectException("connection refused"));
        }

        @GetMapping("/test/crash")
        void crash() {
            throw new RuntimeException("database password leaked");
        }
    }
}
