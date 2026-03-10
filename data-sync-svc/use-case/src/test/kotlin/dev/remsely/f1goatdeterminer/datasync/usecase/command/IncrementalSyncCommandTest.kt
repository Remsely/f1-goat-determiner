package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncCheckpoints
import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

class IncrementalSyncCommandTest {

    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobFinder = mockk<SyncJobFinder>()
    private val syncJobPersister = mockk<SyncJobPersister>()
    private val checkpointFinder = mockk<SyncCheckpointFinder>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)

    private val command = IncrementalSyncCommand(
        syncOrchestrator = syncOrchestrator,
        syncJobFinder = syncJobFinder,
        syncJobPersister = syncJobPersister,
        checkpointFinder = checkpointFinder,
        checkpointPersister = checkpointPersister,
    )

    @Test
    fun `execute creates an INCREMENTAL job and runs orchestrator`() {
        val createdJob = TestSyncJobs.sample(type = SyncJob.Type.INCREMENTAL)
        every { syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) } returns createdJob
        every { syncJobFinder.findLatestCompleted() } returns null

        command.execute()

        verify { syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) }
        verify { syncOrchestrator.execute(createdJob) }
    }

    @Test
    fun `execute seeds checkpoints from previous completed job`() {
        val createdJob = TestSyncJobs.sample(id = 20L, type = SyncJob.Type.INCREMENTAL)
        every { syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) } returns createdJob

        val previousJob = TestSyncJobs.sample(id = 10L, type = SyncJob.Type.INCREMENTAL, status = SyncStatus.COMPLETED)
        every { syncJobFinder.findLatestCompleted() } returns previousJob

        val previousCheckpoints = listOf(
            TestSyncCheckpoints.completed(
                jobId = 10L,
                entityType = SyncEntityType.STATUSES,
                lastOffset = 200,
            ),
            TestSyncCheckpoints.completed(
                jobId = 10L,
                entityType = SyncEntityType.RACE_RESULTS,
                lastOffset = 25900,
                lastSeason = 2025,
                lastRound = 24,
            ),
        )
        every { checkpointFinder.findByJobId(10L) } returns previousCheckpoints

        val savedCheckpoints = slot<List<SyncCheckpoint>>()
        every { checkpointPersister.saveAll(capture(savedCheckpoints)) } answers {
            savedCheckpoints.captured.mapIndexed { i, cp -> cp.copy(id = i.toLong() + 1) }
        }

        command.execute()

        val checkpoints = savedCheckpoints.captured
        checkpoints.size shouldBe SyncEntityType.syncOrdered.size

        val statusesCp = checkpoints.first { it.entityType == SyncEntityType.STATUSES }
        statusesCp.lastOffset shouldBe 200
        statusesCp.recordsSynced shouldBe 0
        statusesCp.status shouldBe SyncStatus.PENDING

        val resultsCp = checkpoints.first { it.entityType == SyncEntityType.RACE_RESULTS }
        resultsCp.lastOffset shouldBe 25900
        resultsCp.lastSeason shouldBe 2025
        resultsCp.lastRound shouldBe 24
        resultsCp.recordsSynced shouldBe 0

        // Entity types not in previous checkpoints should start from zero
        val driversCp = checkpoints.first { it.entityType == SyncEntityType.DRIVERS }
        driversCp.lastOffset shouldBe 0
    }

    @Test
    fun `execute skips if tryCreateJob returns null`() {
        every { syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) } returns null

        command.execute()

        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }

    @Test
    fun `execute fails fast when created incremental job has no id`() {
        val transientJob = TestSyncJobs.sample(id = null, type = SyncJob.Type.INCREMENTAL)
        every { syncJobPersister.tryCreateJob(SyncJob.Type.INCREMENTAL) } returns transientJob

        val error = shouldThrow<IllegalArgumentException> {
            command.execute()
        }

        error.message shouldBe "Required value was null."
        verify(exactly = 0) { checkpointPersister.saveAll(any()) }
        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }
}
