package dev.remsely.f1goatdeterminer.datasync.usecase.command

import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ResumeSyncCommandTest {

    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobPersister = mockk<SyncJobPersister>(relaxed = true)

    private val command = ResumeSyncCommand(syncOrchestrator, syncJobPersister)

    @Test
    fun `execute resumes a claimed job`() {
        val claimedJob = TestSyncJobs.sample(
            errorMessage = "Previous failure",
            totalRequests = 10,
            failedRequests = 1,
        )

        every { syncJobPersister.tryClaimResumableJob(any()) } returns claimedJob

        val result = command.execute()

        result shouldBe true
        verify { syncOrchestrator.execute(claimedJob) }
    }

    @Test
    fun `execute returns false when no resumable jobs found`() {
        every { syncJobPersister.tryClaimResumableJob(any()) } returns null

        val result = command.execute()

        result shouldBe false
        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }

    @Test
    fun `execute passes cooldown to tryClaimResumableJob`() {
        every { syncJobPersister.tryClaimResumableJob(any()) } returns null

        val cooldown = Duration.ofMinutes(10)
        command.execute(cooldown)

        verify { syncJobPersister.tryClaimResumableJob(match { it.isBefore(LocalDateTime.now()) }) }
    }

    @Test
    fun `execute fails fast when claimed resumable job has no id`() {
        val claimedJobWithoutId = TestSyncJobs.sample(
            id = null,
            errorMessage = "Previous failure",
            totalRequests = 10,
            failedRequests = 1,
        )
        every { syncJobPersister.tryClaimResumableJob(any()) } returns claimedJobWithoutId

        val error = shouldThrow<IllegalArgumentException> {
            command.execute()
        }

        error.message shouldBe "Required value was null."
        verify(exactly = 0) { syncOrchestrator.execute(any()) }
    }
}
