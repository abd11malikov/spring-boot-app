package com.epam.trainerworkload.config;

import com.epam.trainerworkload.web.logging.RestCallLoggingFilter;
import com.epam.trainerworkload.web.logging.TransactionIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<TransactionIdFilter> transactionIdFilter() {
        FilterRegistrationBean<TransactionIdFilter> registration = new FilterRegistrationBean<>(new TransactionIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RestCallLoggingFilter> restCallLoggingFilter(
            @Value("${workload.rest-logging.max-body-length}") int maxBodyLength) {
        FilterRegistrationBean<RestCallLoggingFilter> registration =
                new FilterRegistrationBean<>(new RestCallLoggingFilter(maxBodyLength));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
