package com.epam.gym.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.security.SecureRandom;
import java.util.Random;

@Configuration
@ComponentScan(
        basePackages = "com.epam.gym",
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.epam\\.gym\\.web\\..*"))
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Random passwordRandom() {
        return new SecureRandom();
    }
}
