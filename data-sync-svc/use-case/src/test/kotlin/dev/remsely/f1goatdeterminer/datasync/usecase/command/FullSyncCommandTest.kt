package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class FullSyncCommandTest {

    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobPersister = mockk<SyncJobPersister>()

    private val command = FullSyncCommand(syncOrchestrator, syncJobPersister)

    @Test
    fun `execute creates a FULL job and runs orchestrator`() {
        val createdJob = TestSyncJobs.sample(type = SyncJob.Type.FULL)
        every { syncJobPersister.tryCreateJob(SyncJob.Type.FULL) } returns createdJob

        command.execute()

        verify { syncJobPersister.tryCreateJob(SyncJob.Type.FULL) }
        verify { syncOrchestrator.execute(createdJob) }
    }

    @Test
    fun `execute skips if tryCreateJob returns null`() {
        every { syncJobPersister.tryCreateJob(SyncJob.Type.FULL) } returns null

        command.execute()

        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }
}
