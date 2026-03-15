package dev.remsely.f1goatdeterminer.datasync.app

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.postgresql.PostgreSQLContainer

@Configuration
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:18.2-alpine")
            .withDatabaseName("f1_test")
            .withUsername("test")
            .withPassword("test")
}
