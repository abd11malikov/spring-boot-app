package com.epam.gym.security;

import com.epam.gym.MutableClock;
import com.epam.gym.dao.UserDao;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The security filter chains working with the real application context: authentication of every endpoint,
 * login, brute force protection, logout, token expiry, password hashing and CORS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:gym_crm_security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "gym.security.cors.allowed-origins=http://localhost:3000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityEndToEndTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.now());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void registration_isPublic_andReturnsWorkingToken() throws Exception {
        // given
        Account account = register("Open", "Door");

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/" + account.username())
                .header(HttpHeaders.AUTHORIZATION, account.bearer()));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Open"));
    }

    @Test
    void protectedEndpoints_withoutToken_return401WithBearerChallenge() throws Exception {
        // given
        Account account = register("No", "Token");

        // when / then
        for (String url : new String[]{"/api/trainees/" + account.username(), "/api/training-types",
                "/api/trainers/" + account.username() + "/trainings", "/actuator/metrics"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                    .andExpect(jsonPath("$.message").value("Missing bearer token"));
        }
    }

    @Test
    void basicCredentials_areAcceptedOnlyByLogin() throws Exception {
        // given
        Account account = register("Basic", "Only");

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, account.basic()));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void login_returnsTokenThatAuthenticatesRequests() throws Exception {
        // given
        Account account = register("Log", "In");

        // when
        String token = login(account.basic());

        // then
        mockMvc.perform(get("/api/training-types").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_withoutCredentials_returns401WithBasicChallenge() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/auth/login"));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")))
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, basic("Nobody.Here", "whatever")));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void threeWrongPasswords_blockUserForFiveMinutes() throws Exception {
        // given
        Account account = register("Brute", "Force");
        String wrong = basic(account.username(), "wrong-password");
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, wrong))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, wrong))
                .andExpect(status().isUnauthorized());

        // when: blocked, so even the right password is refused
        ResultActions blocked = mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, account.basic()));

        // then
        blocked.andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "300"))
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.BLOCKED));

        // when: 5 minutes later the block is gone
        clock.advance(Duration.ofMinutes(5));

        // then
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, account.basic()))
                .andExpect(status().isOk());
    }

    @Test
    void successfulLogin_resetsFailedAttempts() throws Exception {
        // given
        Account account = register("Second", "Chance");
        String wrong = basic(account.username(), "wrong-password");
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, wrong));
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, wrong));
        login(account.basic());

        // when
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, wrong));

        // then
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, account.basic()))
                .andExpect(status().isOk());
    }

    @Test
    void logout_revokesTheToken() throws Exception {
        // given
        Account account = register("Log", "Out");
        String otherToken = login(account.basic());

        // when
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk());

        // then: the logged out token stops working, other sessions do not
        mockMvc.perform(get("/api/training-types").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been revoked by logout"));
        mockMvc.perform(get("/api/training-types").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withoutToken_returns401() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/logout"));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        // given
        Account account = register("Old", "Token");

        // when
        clock.advance(Duration.ofHours(1).plusSeconds(1));

        // then
        mockMvc.perform(get("/api/training-types").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    void malformedToken_isRejected() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void passwords_areStoredAsSaltedHashes() throws Exception {
        // given
        Account first = register("Same", "Password");
        Account second = register("Same", "Password");

        // when
        String hash = storedPassword(first.username());
        String otherHash = storedPassword(second.username());

        // then
        assertNotEquals(first.password(), hash);
        assertTrue(hash.startsWith("$2"), "BCrypt hash expected");
        assertTrue(passwordEncoder.matches(first.password(), hash));
        assertNotEquals(hash, otherHash);
    }

    @Test
    void corsPreflight_fromAllowedOrigin_isAccepted() throws Exception {
        // when
        ResultActions result = mockMvc.perform(options("/api/trainees/John.Doe")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"));

        // then
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, startsWith("GET")));
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() throws Exception {
        // when
        ResultActions result = mockMvc.perform(options("/api/trainees")
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsHeaders_areAddedToActualResponses() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/trainees")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Cross\",\"lastName\":\"Origin\"}"));

        // then
        result.andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    private Account register(String firstName, String lastName) throws Exception {
        String json = mockMvc.perform(post("/api/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(json, "$.username"), JsonPath.read(json, "$.password"),
                JsonPath.read(json, "$.token"));
    }

    private String storedPassword(String username) {
        return transactionTemplate.execute(status -> userDao.findByUsername(username).orElseThrow().getPassword());
    }

    private String login(String basicHeader) throws Exception {
        String json = mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, basicHeader))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private record Account(String username, String password, String token) {
        String bearer() {
            return "Bearer " + token;
        }

        String basic() {
            return SecurityEndToEndTest.basic(username, password);
        }
    }
}
