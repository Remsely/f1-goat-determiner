package dev.remsely.f1goatdeterminer.datasync.app

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base class for end-to-end integration tests.
 *
 * Provides:
 * - Testcontainers PostgreSQL via [TestcontainersConfig].
 * - WireMock server for mocking Jolpica API (singleton shared across all E2E test classes).
 * - Shared utility methods for database counts, cleanup, and expected counts.
 */
@SpringBootTest
@Import(TestcontainersConfig::class)
@ActiveProfiles("test")
abstract class BaseE2eTest protected constructor() {

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        @JvmStatic
        protected val wireMock: WireMockServer by lazy {
            WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("integration.jolpica.base-url") {
                "http://localhost:${wireMock.port()}"
            }
        }
    }

    @BeforeEach
    fun resetState() {
        cleanDatabase()
        wireMock.resetAll()
    }

    protected fun currentCounts(): Map<String, Long?> = mapOf(
        "statuses" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM statuses"),
        "circuits" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM circuits"),
        "constructors" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM constructors"),
        "drivers" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM drivers"),
        "races" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM races"),
        "results" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM results"),
        "qualifying" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM qualifying"),
        "driver_standings" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM driver_standings"),
        "constructor_standings" to jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM constructor_standings"),
    )

    protected fun expectedCounts(
        statuses: Long = 1,
        circuits: Long = 1,
        constructors: Long = 1,
        drivers: Long = 1,
        races: Long = 1,
        results: Long = 1,
        qualifying: Long = 1,
        driverStandings: Long = 1,
        constructorStandings: Long = 1,
    ): Map<String, Long> = mapOf(
        "statuses" to statuses,
        "circuits" to circuits,
        "constructors" to constructors,
        "drivers" to drivers,
        "races" to races,
        "results" to results,
        "qualifying" to qualifying,
        "driver_standings" to driverStandings,
        "constructor_standings" to constructorStandings,
    )

    protected fun cleanSyncMetadata() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE
                sync_checkpoints,
                sync_jobs
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    protected fun cleanDatabase() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE
                sync_checkpoints,
                sync_jobs,
                constructor_standings,
                driver_standings,
                qualifying,
                results,
                races,
                drivers,
                constructors,
                circuits,
                statuses
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }
}
