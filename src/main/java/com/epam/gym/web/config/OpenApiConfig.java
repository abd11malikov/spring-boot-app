package com.epam.gym.web.config;

import com.epam.gym.dto.Credentials;
import com.epam.gym.web.error.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(Credentials.class);
    }

    @Bean
    public OpenAPI gymCrmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("Trainees, trainers and trainings of the gym CRM system"))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
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
