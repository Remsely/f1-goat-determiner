package dev.remsely.f1goatdeterminer.datasync.app

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.scheduled.SyncScheduler
import dev.remsely.f1goatdeterminer.datasync.usecase.command.FullSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.ResumeSyncCommand
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.queryForObject

/**
 * End-to-end integration tests for the full sync pipeline.
 *
 * Uses:
 * - Testcontainers (PostgreSQL) for real database
 * - WireMock for mocking Jolpica API responses
 * - Real Spring context with all beans wired
 *
 * These tests verify the complete flow: HTTP fetch → mapping → DB persistence → job/checkpoint tracking.
 */
class FullSyncE2eTest : BaseE2eTest() {

    @Autowired
    private lateinit var fullSyncCommand: FullSyncCommand

    @Autowired
    private lateinit var resumeSyncCommand: ResumeSyncCommand

    @Autowired
    private lateinit var syncJobFinder: SyncJobFinder

    @Autowired
    private lateinit var checkpointFinder: SyncCheckpointFinder

    @Autowired
    private lateinit var syncScheduler: SyncScheduler

    @Test
    fun `full sync completes successfully with all entity types`() {
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)

        fullSyncCommand.execute()

        val latestJob = syncJobFinder.findLatest().shouldNotBeNull()
        latestJob.type shouldBe SyncJob.Type.FULL
        latestJob.status shouldBe SyncStatus.COMPLETED
        latestJob.failedRequests shouldBe 0

        val checkpoints = checkpointFinder.findByJobId(latestJob.id!!)
        checkpoints shouldHaveSize SyncEntityType.syncOrdered.size
        checkpoints.forEach { cp -> cp.status shouldBe SyncStatus.COMPLETED }

        currentCounts() shouldContainExactly expectedCounts()
        syncJobFinder.findByStatus(SyncStatus.COMPLETED) shouldHaveSize 1
    }

    @Test
    fun `full sync is idempotent - rerunning produces same data without duplicates`() {
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)

        fullSyncCommand.execute()
        currentCounts() shouldContainExactly expectedCounts()
        val firstRunResultCount = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM results")
        firstRunResultCount shouldBe 1L

        cleanSyncMetadata()

        fullSyncCommand.execute()

        val secondRunResultCount = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM results")
        secondRunResultCount shouldBe 1L
        firstRunResultCount shouldBe secondRunResultCount
        syncJobFinder.findByStatus(SyncStatus.COMPLETED) shouldHaveSize 1
    }

    @Test
    fun `full sync handles API error gracefully - job marked FAILED, not stuck IN_PROGRESS`() {
        JolpicaWireMockStubs.stubStatuses(wireMock)
        JolpicaWireMockStubs.stubCircuits(wireMock)
        JolpicaWireMockStubs.stubConstructors(wireMock)
        JolpicaWireMockStubs.stubDrivers(wireMock)
        JolpicaWireMockStubs.stubAllRaces(wireMock, season = 2024)
        JolpicaWireMockStubs.stubAllResultsError(wireMock)
        JolpicaWireMockStubs.stubAllQualifying(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonDriverStandings(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonConstructorStandings(wireMock, season = 2024)

        fullSyncCommand.execute()

        val job = syncJobFinder.findLatest().shouldNotBeNull()
        job.type shouldBe SyncJob.Type.FULL
        job.status shouldBe SyncStatus.FAILED
        job.failedRequests shouldBe 1

        val checkpoints = checkpointFinder.findByJobId(job.id!!)
        checkpoints shouldHaveSize SyncEntityType.syncOrdered.size

        val raceResultCheckpoint = checkpoints.find { it.entityType == SyncEntityType.RACE_RESULTS }.shouldNotBeNull()
        raceResultCheckpoint.status shouldBe SyncStatus.FAILED
        raceResultCheckpoint.errorMessage.shouldNotBeNull()

        checkpoints.filter { it.entityType != SyncEntityType.RACE_RESULTS }.forEach { cp ->
            cp.status shouldBe SyncStatus.COMPLETED
        }

        currentCounts() shouldContainExactly expectedCounts(results = 0L)
    }

    @Test
    fun `resume after failure completes previously failed checkpoints on the same job`() {
        JolpicaWireMockStubs.stubStatuses(wireMock)
        JolpicaWireMockStubs.stubCircuits(wireMock)
        JolpicaWireMockStubs.stubConstructors(wireMock)
        JolpicaWireMockStubs.stubDrivers(wireMock)
        JolpicaWireMockStubs.stubAllRaces(wireMock, season = 2024)
        JolpicaWireMockStubs.stubAllResultsError(wireMock)
        JolpicaWireMockStubs.stubAllQualifying(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonDriverStandings(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonConstructorStandings(wireMock, season = 2024)

        fullSyncCommand.execute()

        val failedJob = syncJobFinder.findLatest().shouldNotBeNull()
        failedJob.status shouldBe SyncStatus.FAILED
        currentCounts() shouldContainExactly expectedCounts(results = 0L)

        wireMock.resetAll()
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)

        val resumed = resumeSyncCommand.execute()
        resumed shouldBe true

        val resumedJob = syncJobFinder.findLatest().shouldNotBeNull()
        resumedJob.id shouldBe failedJob.id
        resumedJob.status shouldBe SyncStatus.COMPLETED

        val checkpoints = checkpointFinder.findByJobId(failedJob.id!!)
        checkpoints shouldHaveSize SyncEntityType.syncOrdered.size
        checkpoints.forEach { checkpoint -> checkpoint.status shouldBe SyncStatus.COMPLETED }

        val raceResultCheckpoint = checkpoints.find { it.entityType == SyncEntityType.RACE_RESULTS }.shouldNotBeNull()
        raceResultCheckpoint.status shouldBe SyncStatus.COMPLETED
        raceResultCheckpoint.errorMessage shouldBe null
        raceResultCheckpoint.recordsSynced shouldBe 1

        currentCounts() shouldContainExactly expectedCounts()
        val totalJobs = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM sync_jobs")
        totalJobs shouldBe 1L
    }

    @Test
    fun `duplicate full sync creates a second completed job after the first one finishes`() {
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)

        fullSyncCommand.execute()
        val firstJob = syncJobFinder.findLatest().shouldNotBeNull()
        firstJob.status shouldBe SyncStatus.COMPLETED

        fullSyncCommand.execute()

        val completedJobs = syncJobFinder.findByStatus(SyncStatus.COMPLETED)
        completedJobs shouldHaveSize 2
        currentCounts() shouldContainExactly expectedCounts()
    }

    @Test
    fun `runSyncTick recovers stale in progress job and completes same job without creating duplicate`() {
        JolpicaWireMockStubs.stubStatuses(wireMock)
        JolpicaWireMockStubs.stubCircuits(wireMock)
        JolpicaWireMockStubs.stubConstructors(wireMock)
        JolpicaWireMockStubs.stubDrivers(wireMock)
        JolpicaWireMockStubs.stubAllRaces(wireMock, season = 2024)
        JolpicaWireMockStubs.stubAllResultsError(wireMock)
        JolpicaWireMockStubs.stubAllQualifying(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonDriverStandings(wireMock, season = 2024)
        JolpicaWireMockStubs.stubSeasonConstructorStandings(wireMock, season = 2024)

        fullSyncCommand.execute()

        val failedJob = syncJobFinder.findLatest().shouldNotBeNull()
        failedJob.status shouldBe SyncStatus.FAILED
        val originalJobId = failedJob.id.shouldNotBeNull()

        jdbcTemplate.update(
            """
            UPDATE sync_jobs
            SET status = 'IN_PROGRESS',
                updated_at = NOW() - INTERVAL '20 minutes',
                error_message = NULL,
                completed_at = NULL
            WHERE id = ?
            """.trimIndent(),
            originalJobId,
        )

        wireMock.resetAll()
        JolpicaWireMockStubs.setupFullSyncStubs(wireMock)

        syncScheduler.runSyncTick()

        val recoveredJob = syncJobFinder.findLatest().shouldNotBeNull()
        recoveredJob.id shouldBe originalJobId
        recoveredJob.status shouldBe SyncStatus.COMPLETED

        val totalJobs = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM sync_jobs")
        totalJobs shouldBe 1L

        val checkpoints = checkpointFinder.findByJobId(originalJobId)
        checkpoints shouldHaveSize SyncEntityType.syncOrdered.size
        checkpoints.forEach { checkpoint -> checkpoint.status shouldBe SyncStatus.COMPLETED }

        val recoveredResults = checkpoints.find { it.entityType == SyncEntityType.RACE_RESULTS }.shouldNotBeNull()
        recoveredResults.errorMessage shouldBe null
        recoveredResults.recordsSynced shouldBe 1

        currentCounts() shouldContainExactly expectedCounts()
    }
}
