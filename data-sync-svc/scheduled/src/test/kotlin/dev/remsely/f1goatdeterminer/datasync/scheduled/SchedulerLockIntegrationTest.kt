package dev.remsely.f1goatdeterminer.datasync.scheduled

import io.kotest.matchers.shouldBe
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.Instant

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulerLockIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18.2-alpine")
            .withDatabaseName("f1_test")
            .withUsername("test")
            .withPassword("test")
    }

    private val dataSource by lazy {
        DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }
    }

    private val jdbcTemplate by lazy { JdbcTemplate(dataSource) }

    private val lockProvider by lazy {
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build(),
        )
    }

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS shedlock (
                name VARCHAR(64) NOT NULL,
                lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
                locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
                locked_by VARCHAR(255) NOT NULL,
                CONSTRAINT pk_shedlock_name PRIMARY KEY (name)
            )
            """.trimIndent(),
        )
        jdbcTemplate.execute("TRUNCATE TABLE shedlock")
    }

    @Test
    fun `second lock acquisition fails while first lock is held`() {
        val lockConfig = LockConfiguration(
            Instant.now(),
            "syncScheduler.syncTick",
            Duration.ofMinutes(1),
            Duration.ZERO,
        )

        val firstLock = lockProvider.lock(lockConfig)
        val secondLock = lockProvider.lock(lockConfig)

        firstLock.isPresent shouldBe true
        secondLock.isPresent shouldBe false

        firstLock.get().unlock()

        val thirdLock = lockProvider.lock(lockConfig)
        thirdLock.isPresent shouldBe true
        thirdLock.get().unlock()
    }
}
