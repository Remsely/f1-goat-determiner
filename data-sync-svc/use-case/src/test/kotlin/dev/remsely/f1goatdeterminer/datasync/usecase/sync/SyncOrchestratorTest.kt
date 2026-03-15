package dev.remsely.f1goatdeterminer.datasync.usecase.sync

import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncerRegistry
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SyncOrchestratorTest {

    private val syncerRegistry = mockk<EntitySyncerRegistry>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val checkpointFinder = mockk<SyncCheckpointFinder>()
    private val jobPersister = mockk<SyncJobPersister>(relaxed = true)

    private val orchestrator = SyncOrchestrator(
        syncerRegistry,
        checkpointPersister,
        checkpointFinder,
        jobPersister,
    )

    private val testJob = TestSyncJobs.sample()

    @Test
    fun `execute creates checkpoints when none exist and completes successfully`() {
        val checkpoints = SyncEntityType.syncOrdered.mapIndexed { index, entityType ->
            SyncCheckpoint.initPending(1L, entityType).copy(id = index.toLong() + 1)
        }

        every { checkpointFinder.findByJobId(1L) } returns emptyList()
        every { checkpointPersister.saveAll(any()) } returns checkpoints

        for (entityType in SyncEntityType.syncOrdered) {
            val syncer = mockk<EntitySyncer>()
            every { syncer.entityType } returns entityType
            every { syncer.sync(any()) } returns SyncResult(
                recordsSynced = 10,
                lastOffset = 100,
                lastSeason = null,
                lastRound = null,
                apiCallsMade = 1,
            )
            every { syncerRegistry.getSyncer(entityType) } returns syncer
        }

        orchestrator.execute(testJob)

        verify { jobPersister.updateStatus(1L, SyncStatus.IN_PROGRESS) }
        verify { jobPersister.complete(1L, SyncStatus.COMPLETED, any()) }
    }

    @Test
    fun `execute marks job as FAILED when a syncer throws SyncException`() {
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val failingSyncer = mockk<EntitySyncer>()
        every { failingSyncer.entityType } returns SyncEntityType.STATUSES
        every { failingSyncer.sync(any()) } throws SyncException("API error")
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns failingSyncer

        orchestrator.execute(testJob)

        verify { checkpointPersister.fail(1L, "Failed to sync STATUSES: API error") }
        verify { checkpointPersister.incrementRetryCount(1L) }
        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute marks job as FAILED when a syncer throws unexpected RuntimeException`() {
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val failingSyncer = mockk<EntitySyncer>()
        every { failingSyncer.entityType } returns SyncEntityType.STATUSES
        every { failingSyncer.sync(any()) } throws RuntimeException("Unexpected error")
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns failingSyncer

        orchestrator.execute(testJob)

        verify { checkpointPersister.fail(1L, "Failed to sync STATUSES: Unexpected error") }
        verify { checkpointPersister.incrementRetryCount(1L) }
        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute skips already completed checkpoints`() {
        val completedCheckpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES)
            .copy(id = 1L, status = SyncStatus.COMPLETED)
        val pendingCheckpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS)
            .copy(id = 2L)

        every { checkpointFinder.findByJobId(1L) } returns listOf(completedCheckpoint, pendingCheckpoint)

        val circuitSyncer = mockk<EntitySyncer>()
        every { circuitSyncer.entityType } returns SyncEntityType.CIRCUITS
        every { circuitSyncer.sync(any()) } returns SyncResult(5, 50, null, null, 1)
        every { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) } returns circuitSyncer

        orchestrator.execute(testJob)

        verify(exactly = 0) { syncerRegistry.getSyncer(SyncEntityType.STATUSES) }
        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) }
    }

    @Test
    fun `execute retries previously FAILED checkpoints with reset retry count on resume`() {
        val exhaustedCheckpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES)
            .copy(id = 1L, status = SyncStatus.FAILED, retryCount = SyncCheckpoint.MAX_RETRIES)
        val pendingCheckpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS)
            .copy(id = 2L)

        every { checkpointFinder.findByJobId(1L) } returns listOf(exhaustedCheckpoint, pendingCheckpoint)

        val statusSyncer = mockk<EntitySyncer>()
        every { statusSyncer.entityType } returns SyncEntityType.STATUSES
        every { statusSyncer.sync(any()) } returns SyncResult(5, 50, null, null, 1)
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns statusSyncer

        val circuitSyncer = mockk<EntitySyncer>()
        every { circuitSyncer.entityType } returns SyncEntityType.CIRCUITS
        every { circuitSyncer.sync(any()) } returns SyncResult(5, 50, null, null, 1)
        every { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) } returns circuitSyncer

        orchestrator.execute(testJob)

        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.STATUSES) }
        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) }
        verify { checkpointPersister.resetRetryCount(1L) }
    }

    @Test
    fun `execute guarantees job status update even on unexpected error in ensureCheckpoints`() {
        every { checkpointFinder.findByJobId(1L) } throws RuntimeException("DB connection failed")

        orchestrator.execute(testJob)

        verify { jobPersister.updateStatus(1L, SyncStatus.IN_PROGRESS) }
        verify { jobPersister.updateStatus(1L, SyncStatus.FAILED, "DB connection failed") }
        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute skips dependent checkpoints when a dependency fails`() {
        // GRAND_PRIX depends on CIRCUITS. If CIRCUITS fails, GRAND_PRIX should be skipped.
        // RACE_RESULTS depends on GRAND_PRIX. If GRAND_PRIX is skipped, RACE_RESULTS should also be skipped.
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L),
            SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 2L),
            SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 3L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val failingSyncer = mockk<EntitySyncer>()
        every { failingSyncer.entityType } returns SyncEntityType.CIRCUITS
        every { failingSyncer.sync(any()) } throws SyncException("API error")
        every { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) } returns failingSyncer

        orchestrator.execute(testJob)

        // CIRCUITS failed
        verify { checkpointPersister.fail(1L, "Failed to sync CIRCUITS: API error") }

        // GRAND_PRIX skipped because CIRCUITS failed
        verify { checkpointPersister.updateStatus(2L, SyncStatus.SKIPPED, "Dependency failed: [CIRCUITS]") }
        verify(exactly = 0) { syncerRegistry.getSyncer(SyncEntityType.GRAND_PRIX) }

        // RACE_RESULTS skipped because GRAND_PRIX is also in failedEntityTypes
        verify { checkpointPersister.updateStatus(3L, SyncStatus.SKIPPED, any()) }
        verify(exactly = 0) { syncerRegistry.getSyncer(SyncEntityType.RACE_RESULTS) }

        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute runs dependent checkpoint when its dependencies all succeed`() {
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L),
            SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 2L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        for (entityType in listOf(SyncEntityType.CIRCUITS, SyncEntityType.GRAND_PRIX)) {
            val syncer = mockk<EntitySyncer>()
            every { syncer.entityType } returns entityType
            every { syncer.sync(any()) } returns SyncResult(10, 100, null, null, 1)
            every { syncerRegistry.getSyncer(entityType) } returns syncer
        }

        orchestrator.execute(testJob)

        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) }
        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.GRAND_PRIX) }
        verify { jobPersister.complete(1L, SyncStatus.COMPLETED, any()) }
    }

    @Test
    fun `execute skips independent entity when another fails but does not skip unrelated entities`() {
        // STATUSES fails. CIRCUITS has no dependency on STATUSES, so should run fine.
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L),
            SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 2L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val failingSyncer = mockk<EntitySyncer>()
        every { failingSyncer.entityType } returns SyncEntityType.STATUSES
        every { failingSyncer.sync(any()) } throws SyncException("error")
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns failingSyncer

        val circuitSyncer = mockk<EntitySyncer>()
        every { circuitSyncer.entityType } returns SyncEntityType.CIRCUITS
        every { circuitSyncer.sync(any()) } returns SyncResult(10, 100, null, null, 1)
        every { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) } returns circuitSyncer

        orchestrator.execute(testJob)

        // STATUSES failed but CIRCUITS is independent — should still run
        verify(exactly = 1) { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) }
        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute stops entire job when RateLimitExhaustedException is thrown`() {
        // STATUSES throws RateLimitExhaustedException — no further checkpoints should be attempted.
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L),
            SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 2L),
            SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 3L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val rateLimitedSyncer = mockk<EntitySyncer>()
        every { rateLimitedSyncer.entityType } returns SyncEntityType.STATUSES
        every { rateLimitedSyncer.sync(any()) } throws RateLimitExhaustedException("429 exhausted")
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns rateLimitedSyncer

        orchestrator.execute(testJob)

        // STATUSES failed due to rate limit
        verify { checkpointPersister.fail(1L, "Failed to sync STATUSES: 429 exhausted") }

        // Rate limit errors should NOT increment retry count (transient, not permanent)
        verify(exactly = 0) { checkpointPersister.incrementRetryCount(1L) }

        // CIRCUITS and DRIVERS should NOT be attempted — job stopped immediately
        verify(exactly = 0) { syncerRegistry.getSyncer(SyncEntityType.CIRCUITS) }
        verify(exactly = 0) { syncerRegistry.getSyncer(SyncEntityType.DRIVERS) }

        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
    }

    @Test
    fun `execute fails fast when sync job has no id`() {
        val transientJob = testJob.copy(id = null)

        val error = shouldThrow<IllegalArgumentException> {
            orchestrator.execute(transientJob)
        }

        error.message shouldBe "SyncJob must be persisted before orchestration"
        verify(exactly = 0) { jobPersister.updateStatus(any(), any(), any()) }
        verify(exactly = 0) { jobPersister.complete(any(), any(), any()) }
    }

    @Test
    fun `execute marks job as failed when checkpoint is missing id`() {
        every { checkpointFinder.findByJobId(1L) } returns listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES),
        )

        orchestrator.execute(testJob)

        verify { jobPersister.updateStatus(1L, SyncStatus.IN_PROGRESS) }
        verify { jobPersister.updateStatus(1L, SyncStatus.FAILED, "Checkpoint must be persisted") }
        verify { jobPersister.complete(1L, SyncStatus.FAILED, any()) }
        verify(exactly = 0) { syncerRegistry.getSyncer(any()) }
    }
}
