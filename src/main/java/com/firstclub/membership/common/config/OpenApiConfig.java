package com.firstclub.membership.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI membershipOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FirstClub Membership Service")
                .version("1.0.0")
                .description("Subscription-based memberships with configurable tiered benefits."));
    }
}
