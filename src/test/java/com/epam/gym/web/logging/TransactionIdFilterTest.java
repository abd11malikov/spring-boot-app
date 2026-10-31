package com.epam.gym.web.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionIdFilterTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final TransactionIdFilter filter = new TransactionIdFilter();

    @Test
    void doFilter_withoutIncomingId_generatesUuidAndReturnsItInHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenInsideChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> idSeenInsideChain.set(MDC.get(TransactionIdFilter.MDC_KEY));

        // when
        filter.doFilter(request, response, chain);

        // then
        String headerId = response.getHeader(TransactionIdFilter.HEADER);
        assertTrue(headerId.matches(UUID_PATTERN), "Not a UUID: " + headerId);
        assertEquals(headerId, idSeenInsideChain.get());
    }

    @Test
    void doFilter_withValidIncomingId_reusesIt() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        request.addHeader(TransactionIdFilter.HEADER, "order-service-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenInsideChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> idSeenInsideChain.set(MDC.get(TransactionIdFilter.MDC_KEY));

        // when
        filter.doFilter(request, response, chain);

        // then
        assertEquals("order-service-42", response.getHeader(TransactionIdFilter.HEADER));
        assertEquals("order-service-42", idSeenInsideChain.get());
    }

    @Test
    void doFilter_withUnsafeIncomingId_replacesIt() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        request.addHeader(TransactionIdFilter.HEADER, "evil id\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> { });

        // then
        String headerId = response.getHeader(TransactionIdFilter.HEADER);
        assertNotEquals("evil id\nFAKE LOG LINE", headerId);
        assertTrue(headerId.matches(UUID_PATTERN));
    }

    @Test
    void doFilter_tooLongIncomingId_isReplaced() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        request.addHeader(TransactionIdFilter.HEADER, "a".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> { });

        // then
        assertTrue(response.getHeader(TransactionIdFilter.HEADER).matches(UUID_PATTERN));
    }

    @Test
    void doFilter_afterRequest_clearsMdcEvenWhenChainFails() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failingChain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        // when
        try {
            filter.doFilter(request, response, failingChain);
        } catch (Exception ignored) {
            // the failure itself is not under test
        }

        // then
        assertNull(MDC.get(TransactionIdFilter.MDC_KEY));
    }
}
