package com.tcc.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.streaming.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for E2E tests with full infrastructure (PostgreSQL + Redis).
 * Tests the application with Redis cache enabled to catch serialization issues.
 * 
 * Use this class when you need to test:
 * - Cache behavior with real Redis
 * - JSON serialization/deserialization through Redis
 * - Full integration with all dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-redis")
@Testcontainers
@Import(TestConfig.class)
public abstract class AbstractE2ETestWithRedis {

    @Container
    protected static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test")
        .withReuse(false);

    @Container
    protected static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(false);

    @LocalServerPort
    protected int port;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String baseUrl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        
        // Redis
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("redis.enabled", () -> "true");
        
        // RabbitMQ still disabled (not needed for cache testing)
        registry.add("rabbitmq.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Clean Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    /**
     * Helper method to build API URL.
     */
    protected String apiUrl(String path) {
        return baseUrl + "/api" + path;
    }
}
