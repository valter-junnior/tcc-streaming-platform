package com.tcc.streaming.common.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI streamingPlatformOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Streaming Platform API")
                .description("API REST para plataforma de streaming de vídeo ao vivo com análise comparativa de desempenho. " +
                            "Desenvolvido com Clean Architecture usando Spring Boot 3.2, PostgreSQL, Redis, RabbitMQ e WebSocket.")
                .version("1.0.0-SNAPSHOT")
                .contact(new Contact()
                    .name("TCC - Streaming Platform")
                    .email("contato@streaming-platform.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Servidor de Desenvolvimento"),
                new Server()
                    .url("http://localhost:8080")
                    .description("Servidor de Produção")
            ));
    }
}
