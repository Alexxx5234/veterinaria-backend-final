package com.veterinaria.veterinaria.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class OpenApiConfig {



    @Bean
    public OpenAPI customOpenAPI(){


        return new OpenAPI()

                .info(
                    new Info()
                    .title("Veterinaria API")
                    .version("1.0")
                    .description(
                        "API con autenticación JWT y roles USER/ADMIN"
                    )
                )


                .addSecurityItem(
                    new SecurityRequirement()
                    .addList("Bearer Authentication")
                )


                .components(

                    new Components()

                    .addSecuritySchemes(
                        "Bearer Authentication",

                        new SecurityScheme()

                        .name("Bearer Authentication")

                        .type(SecurityScheme.Type.HTTP)

                        .scheme("bearer")

                        .bearerFormat("JWT")
                    )

                );


    }

}