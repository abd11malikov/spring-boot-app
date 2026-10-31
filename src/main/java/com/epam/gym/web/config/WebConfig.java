package com.epam.gym.web.config;

import com.epam.gym.web.auth.CredentialsArgumentResolver;
import com.epam.gym.web.logging.RestCallLoggingFilter;
import com.epam.gym.web.logging.TransactionIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CredentialsArgumentResolver credentialsArgumentResolver;

    public WebConfig(CredentialsArgumentResolver credentialsArgumentResolver) {
        this.credentialsArgumentResolver = credentialsArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(credentialsArgumentResolver);
    }

    @Bean
    public FilterRegistrationBean<TransactionIdFilter> transactionIdFilter() {
        FilterRegistrationBean<TransactionIdFilter> registration = new FilterRegistrationBean<>(new TransactionIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RestCallLoggingFilter> restCallLoggingFilter(
            @Value("${gym.rest-logging.max-body-length}") int maxBodyLength) {
        FilterRegistrationBean<RestCallLoggingFilter> registration =
                new FilterRegistrationBean<>(new RestCallLoggingFilter(maxBodyLength));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
