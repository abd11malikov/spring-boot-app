package com.epam.gym.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Random;

@Configuration
public class AppConfig {

    @Bean
    public Random passwordRandom() {
        return new SecureRandom();
    }
}
