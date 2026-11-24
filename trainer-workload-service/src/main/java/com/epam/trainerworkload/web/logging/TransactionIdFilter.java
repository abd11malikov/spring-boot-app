package com.epam.trainerworkload.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Transaction level logging: keeps the transaction id Gym CRM sends in {@value #HEADER} (or generates one),
 * so one id follows an operation across both services' logs.
 */
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "transactionId";
    public static final String HEADER = "X-Transaction-Id";

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String transactionId = resolveTransactionId(request.getHeader(HEADER));
        MDC.put(MDC_KEY, transactionId);
        response.setHeader(HEADER, transactionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveTransactionId(String incoming) {
        if (incoming != null && VALID_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
