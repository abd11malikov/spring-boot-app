package com.epam.gym.web.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestCallLoggingFilterTest {

    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(RestCallLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(appender);
    }

    @Test
    void successfulCall_isLoggedAtInfoWithEndpointStatusAndMaskedBodies() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(2000);
        MockHttpServletRequest request = jsonRequest("POST", "/api/trainees", "{\"firstName\":\"John\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = respondWith(201, "{\"username\":\"John.Doe\",\"password\":\"aB3dE5gH7j\"}");

        // when
        filter.doFilter(request, response, chain);

        // then
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.startsWith("REST POST /api/trainees -> 201 in "), message);
        assertTrue(message.contains("request: {\"firstName\":\"John\"}"), message);
        assertTrue(message.contains("\"password\":\"****\""), message);
        assertFalse(message.contains("aB3dE5gH7j"), message);
    }

    @Test
    void responseBody_isStillDeliveredToClientUnmasked() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(2000);
        MockHttpServletRequest request = jsonRequest("POST", "/api/trainees", "{\"firstName\":\"John\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String body = "{\"username\":\"John.Doe\",\"password\":\"aB3dE5gH7j\"}";

        // when
        filter.doFilter(request, response, respondWith(201, body));

        // then
        assertEquals(body, response.getContentAsString());
    }

    @Test
    void clientError_isLoggedAtWarnWithQueryString() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(2000);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainees/John.Doe/trainings");
        request.setQueryString("periodFrom=yesterday");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, respondWith(400, "{\"message\":\"bad\"}"));

        // then
        ILoggingEvent event = singleEvent();
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage()
                .startsWith("REST GET /api/trainees/John.Doe/trainings?periodFrom=yesterday -> 400"));
    }

    @Test
    void serverError_isLoggedAtError() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(2000);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, respondWith(500, "{\"message\":\"Unexpected server error\"}"));

        // then
        assertEquals(Level.ERROR, singleEvent().getLevel());
    }

    @Test
    void emptyBodies_areLoggedAsDash() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(2000);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(200));

        // then
        assertTrue(singleEvent().getFormattedMessage().endsWith("| request: - | response: -"));
    }

    @Test
    void longBody_isTruncated() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(20);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, respondWith(200, "[" + "\"Yoga\",".repeat(50) + "\"Cardio\"]"));

        // then
        assertTrue(singleEvent().getFormattedMessage().endsWith("...(truncated)"));
    }

    @Test
    void constructor_nonPositiveLength_throwsException() {
        // given
        int maxBodyLength = 0;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RestCallLoggingFilter(maxBodyLength));

        // then
        assertEquals("Max body length must be positive", exception.getMessage());
    }

    private ILoggingEvent singleEvent() {
        assertEquals(1, appender.list.size(), "Expected exactly one log line: " + appender.list);
        return appender.list.get(0);
    }

    private MockHttpServletRequest jsonRequest(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private FilterChain respondWith(int status, String body) {
        return (req, res) -> {
            StreamUtils.copyToByteArray(req.getInputStream());
            HttpServletResponse httpResponse = (HttpServletResponse) res;
            httpResponse.setStatus(status);
            httpResponse.setContentType("application/json");
            httpResponse.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        };
    }
}
