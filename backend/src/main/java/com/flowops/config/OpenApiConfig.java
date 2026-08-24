package com.flowops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI / Swagger UI metadata. Swagger UI is served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flowOpsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("FlowOps API")
                .description("AI workflow automation platform — build workflows, let AI run the work.")
                .version("0.1.0")
                .contact(new Contact().name("FlowOps"))
                .license(new License().name("Proprietary")));
    }
}
