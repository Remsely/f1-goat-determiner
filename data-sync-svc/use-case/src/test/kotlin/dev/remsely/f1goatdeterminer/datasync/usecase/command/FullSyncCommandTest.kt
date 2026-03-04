package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FullSyncCommandTest {

    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobFinder = mockk<SyncJobFinder>()
    private val syncJobPersister = mockk<SyncJobPersister>()

    private val command = FullSyncCommand(syncOrchestrator, syncJobFinder, syncJobPersister)

    @Test
    fun `execute creates a FULL job and runs orchestrator`() {
        every { syncJobFinder.findActiveByType(SyncJob.Type.FULL) } returns null

        val savedJob = slot<SyncJob>()
        every { syncJobPersister.save(capture(savedJob)) } answers {
            savedJob.captured.copy(id = 1L)
        }

        command.execute()

        verify { syncJobPersister.save(any()) }
        verify { syncOrchestrator.execute(any()) }
    }

    @Test
    fun `execute skips if there is already an active FULL job`() {
        val activeJob = SyncJob(
            id = 1L,
            type = SyncJob.Type.FULL,
            status = SyncStatus.IN_PROGRESS,
            startedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            completedAt = null,
            errorMessage = null,
            totalRequests = 0,
            failedRequests = 0,
        )

        every { syncJobFinder.findActiveByType(SyncJob.Type.FULL) } returns activeJob

        command.execute()

        verify(exactly = 0) { syncJobPersister.save(any()) }
        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }
}
