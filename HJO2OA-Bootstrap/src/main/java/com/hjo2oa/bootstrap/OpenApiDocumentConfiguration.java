package com.hjo2oa.bootstrap;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiDocumentConfiguration {

    @Bean
    public OpenAPI hjo2oaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HJO2OA API")
                        .version("v1")
                        .description("HJO2OA unified OpenAPI document"))
                .servers(List.of(new Server().url("/").description("Current deployment")));
    }
}
