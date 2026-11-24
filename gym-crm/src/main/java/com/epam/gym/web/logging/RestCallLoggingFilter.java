package com.epam.gym.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RestCallLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RestCallLoggingFilter.class);

    private final int maxBodyLength;

    public RestCallLoggingFilter(int maxBodyLength) {
        if (maxBodyLength <= 0) {
            throw new IllegalArgumentException("Max body length must be positive");
        }
        this.maxBodyLength = maxBodyLength;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, maxBodyLength);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logCall(cachedRequest, cachedResponse, duration);
            cachedResponse.copyBodyToResponse();
        }
    }

    private void logCall(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                         long duration) {
        String endpoint = request.getMethod() + " " + request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String requestBody = body(request.getContentAsByteArray());
        String responseBody = body(response.getContentAsByteArray());
        int status = response.getStatus();
        String format = "REST {} -> {} in {} ms | request: {} | response: {}";

        if (status >= 500) {
            log.error(format, endpoint, status, duration, requestBody, responseBody);
        } else if (status >= 400) {
            log.warn(format, endpoint, status, duration, requestBody, responseBody);
        } else {
            log.info(format, endpoint, status, duration, requestBody, responseBody);
        }
    }

    private String body(byte[] content) {
        if (content.length == 0) {
            return "-";
        }
        String text = SensitiveDataMasker.mask(new String(content, StandardCharsets.UTF_8));
        if (text.length() > maxBodyLength) {
            text = text.substring(0, maxBodyLength) + "...(truncated)";
        }
        return text;
    }
}
