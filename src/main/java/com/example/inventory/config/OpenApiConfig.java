package com.example.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI inventoryOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Inventory API")
                        .version("v1")
                        .description("Four CRUD modules: Category, Supplier, Product, and Order. "
                                + "Designed for Apigee proxy publishing and 42Crunch API contract review."));
    }
}
