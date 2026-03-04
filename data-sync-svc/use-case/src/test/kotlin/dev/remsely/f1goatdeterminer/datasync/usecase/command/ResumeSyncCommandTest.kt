package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ResumeSyncCommandTest {

    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobFinder = mockk<SyncJobFinder>()
    private val syncJobPersister = mockk<SyncJobPersister>(relaxed = true)

    private val command = ResumeSyncCommand(syncOrchestrator, syncJobFinder, syncJobPersister)

    @Test
    fun `execute resumes a failed job`() {
        val failedJob = SyncJob(
            id = 1L,
            type = SyncJob.Type.FULL,
            status = SyncStatus.FAILED,
            startedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            completedAt = null,
            errorMessage = "Previous failure",
            totalRequests = 10,
            failedRequests = 1,
        )

        every {
            syncJobFinder.findByStatusIn(listOf(SyncStatus.FAILED, SyncStatus.PAUSED))
        } returns listOf(failedJob)

        val result = command.execute()

        result shouldBe true
        verify { syncJobPersister.updateStatus(1L, SyncStatus.PENDING) }
        verify { syncOrchestrator.execute(any()) }
    }

    @Test
    fun `execute returns false when no resumable jobs found`() {
        every {
            syncJobFinder.findByStatusIn(listOf(SyncStatus.FAILED, SyncStatus.PAUSED))
        } returns emptyList()

        val result = command.execute()

        result shouldBe false
        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }
}
