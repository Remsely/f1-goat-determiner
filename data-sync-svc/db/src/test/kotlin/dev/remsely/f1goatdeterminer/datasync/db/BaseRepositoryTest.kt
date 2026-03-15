package dev.remsely.f1goatdeterminer.datasync.db

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base class for integration tests that need a real PostgreSQL database.
 *
 * Provides:
 * - PostgreSQL Testcontainer shared across all test classes (singleton pattern).
 * - Flyway migrations applied automatically.
 * - Spring Data JPA auto-configuration.
 * - Kotest assertions available in subclasses.
 *
 * Subclasses should annotate with [@Import] to register DAO/JDBC beans
 * that are not auto-discovered by [@DataJpaTest].
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@ActiveProfiles("test")
abstract class BaseRepositoryTest protected constructor() {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18.2-alpine")
            .withDatabaseName("f1_test")
            .withUsername("test")
            .withPassword("test")
    }
}
