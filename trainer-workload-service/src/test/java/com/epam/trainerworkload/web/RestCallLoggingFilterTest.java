package com.epam.trainerworkload.web;

import com.epam.trainerworkload.web.logging.RestCallLoggingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestCallLoggingFilterTest {

    @Test
    void responseBody_isStillDeliveredAfterLogging() throws Exception {
        // given
        RestCallLoggingFilter filter = new RestCallLoggingFilter(5);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        request.setContent("{\"long\":\"body\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req,
                                   jakarta.servlet.http.HttpServletResponse res) throws java.io.IOException {
                req.getInputStream().readAllBytes();
                res.setStatus(500);
                res.getWriter().write("{\"error\":\"x\"}");
            }
        }));

        // then
        assertEquals(500, response.getStatus());
        assertEquals("{\"error\":\"x\"}", response.getContentAsString());
    }

    @Test
    void nonPositiveBodyLimit_isRefused() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new RestCallLoggingFilter(0));
    }
}
