package com.epam.gym.web.config;

import com.epam.gym.security.SecurityConfig;
import com.epam.gym.web.error.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI gymCrmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("Trainees, trainers and trainings of the gym CRM system"))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("basic")
                                .description("Username and password, only for login"))
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token returned by registration or login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Logout is handled by Spring Security's logout filter, not by a controller, so springdoc cannot see it.
     */
    @Bean
    public OpenApiCustomizer logoutEndpointCustomizer() {
        return openApi -> openApi.path(SecurityConfig.LOGOUT_URL, new PathItem().post(new Operation()
                .addTagsItem("Authentication")
                .summary("Logout")
                .description("Revokes the bearer token the request is sent with")
                .operationId("logout")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Logged out, the token no longer works"))
                        .addApiResponse("401", new ApiResponse().description("Missing, invalid or expired token")))));
    }

    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().read(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);
            Content errorContent = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                operation.getResponses().putIfAbsent("500",
                        new ApiResponse().description("Unexpected server error"));
                operation.getResponses().forEach((code, response) -> {
                    if (code.startsWith("4") || code.startsWith("5")) {
                        response.setContent(errorContent);
                    }
                });
            }));
        };
    }
}
