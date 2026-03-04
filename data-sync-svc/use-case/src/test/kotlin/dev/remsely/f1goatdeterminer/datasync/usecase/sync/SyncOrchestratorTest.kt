package dev.remsely.f1goatdeterminer.datasync.usecase.sync

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncer
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.EntitySyncerRegistry
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.SyncResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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

    private val testJob = SyncJob(
        id = 1L,
        type = SyncJob.Type.FULL,
        status = SyncStatus.PENDING,
        startedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        completedAt = null,
        errorMessage = null,
        totalRequests = 0,
        failedRequests = 0,
    )

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
    fun `execute marks job as FAILED when a syncer throws exception`() {
        val checkpoints = listOf(
            SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L),
        )

        every { checkpointFinder.findByJobId(1L) } returns checkpoints

        val failingSyncer = mockk<EntitySyncer>()
        every { failingSyncer.entityType } returns SyncEntityType.STATUSES
        every { failingSyncer.sync(any()) } throws IllegalStateException("API error")
        every { syncerRegistry.getSyncer(SyncEntityType.STATUSES) } returns failingSyncer

        orchestrator.execute(testJob)

        verify { checkpointPersister.fail(1L, "Sync failed for STATUSES: API error") }
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
}
