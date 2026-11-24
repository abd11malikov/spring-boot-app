package com.epam.trainerworkload.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String SERVICE_TOKEN = "serviceToken";

    @Bean
    public OpenAPI trainerWorkloadOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trainer Workload REST API")
                        .version("1.0")
                        .description("Monthly training minutes of each trainer, updated by Gym CRM"))
                .components(new Components().addSecuritySchemes(SERVICE_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Service token signed by Gym CRM")))
                .addSecurityItem(new SecurityRequirement().addList(SERVICE_TOKEN));
    }
}
