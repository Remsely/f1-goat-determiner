package dev.remsely.f1goatdeterminer.datasync.scheduled

import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.command.FullSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.IncrementalSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.ResumeSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Optional

class SyncSchedulerTest {

    private val fullSyncCommand = mockk<FullSyncCommand>(relaxed = true)
    private val incrementalSyncCommand = mockk<IncrementalSyncCommand>(relaxed = true)
    private val resumeSyncCommand = mockk<ResumeSyncCommand>(relaxed = true)
    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true)
    private val syncJobFinder = mockk<SyncJobFinder>()
    private val syncJobPersister = mockk<SyncJobPersister>(relaxed = true)
    private val lockProvider = mockk<LockProvider>()

    init {
        // By default, lock acquisition succeeds (simulates acquiring the startup lock)
        val simpleLock = mockk<SimpleLock>(relaxed = true)
        every { lockProvider.lock(any()) } returns Optional.of(simpleLock)
    }

    private fun createScheduler(
        properties: SyncSchedulerProperties = testProperties(),
    ) = SyncScheduler(
        fullSyncCommand = fullSyncCommand,
        incrementalSyncCommand = incrementalSyncCommand,
        resumeSyncCommand = resumeSyncCommand,
        syncOrchestrator = syncOrchestrator,
        syncJobFinder = syncJobFinder,
        syncJobPersister = syncJobPersister,
        lockProvider = lockProvider,
        properties = properties,
    )

    @Test
    fun `runSyncTick starts full sync when no previous jobs exist`() {
        val scheduler = createScheduler(testProperties(fullOnFirstRun = true))

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findByStatusIn(any()) } returns emptyList()
        every { syncJobFinder.findLatest() } returns null

        scheduler.runSyncTick()

        verify(exactly = 1) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick starts incremental sync when completed full sync exists`() {
        val scheduler = createScheduler()

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findByStatusIn(any()) } returns emptyList()
        every { syncJobFinder.findLatest() } returns TestSyncJobs.sample(status = SyncStatus.COMPLETED)
        every { syncJobFinder.hasCompletedFullSync() } returns true

        scheduler.runSyncTick()

        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 1) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick skips incremental when no completed full sync`() {
        val scheduler = createScheduler()
        val failedJob = TestSyncJobs.sample(status = SyncStatus.FAILED)

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findByStatusIn(any()) } returns emptyList()
        every { syncJobFinder.findLatest() } returns failedJob
        every { syncJobFinder.hasCompletedFullSync() } returns false

        scheduler.runSyncTick()

        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick skips when failed job is in cooldown`() {
        val scheduler = createScheduler()
        val failedJob = TestSyncJobs.sample(status = SyncStatus.FAILED)

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findByStatusIn(any()) } returns listOf(failedJob)

        scheduler.runSyncTick()

        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick resumes failed job instead of starting incremental`() {
        val scheduler = createScheduler()

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns true

        scheduler.runSyncTick()

        verify(exactly = 1) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick recovers stale job before checking active jobs`() {
        val scheduler = createScheduler()
        val staleJob = TestSyncJobs.sample(status = SyncStatus.PENDING)

        every { syncJobPersister.tryClaimStaleJob(any()) } returns staleJob

        scheduler.runSyncTick()

        verify(exactly = 1) { syncOrchestrator.execute(staleJob) }
        verify(exactly = 0) { syncJobFinder.hasActiveJobs() }
        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup acquires lock and invokes resume command when enabled`() {
        val scheduler = createScheduler(testProperties(resumeOnStartup = true))

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { resumeSyncCommand.execute(any()) } returns true

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 1) { lockProvider.lock(any()) }
        verify(exactly = 1) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup skips when lock not acquired`() {
        val scheduler = createScheduler(testProperties(resumeOnStartup = true))

        every { lockProvider.lock(any()) } returns Optional.empty()

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 0) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup recovers stale job before trying resume`() {
        val scheduler = createScheduler(testProperties(resumeOnStartup = true))

        val staleJob = TestSyncJobs.sample(status = SyncStatus.PENDING)
        every { syncJobPersister.tryClaimStaleJob(any()) } returns staleJob

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 1) { syncOrchestrator.execute(staleJob) }
        verify(exactly = 0) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup runs full sync when no jobs exist and fullOnFirstRun enabled`() {
        val scheduler = createScheduler(
            testProperties(resumeOnStartup = true, fullOnFirstRun = true),
        )

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findLatest() } returns null

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 1) { resumeSyncCommand.execute(any()) }
        verify(exactly = 1) { fullSyncCommand.execute() }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup skips when resume disabled and fullOnFirstRun disabled`() {
        val scheduler = createScheduler(
            testProperties(resumeOnStartup = false, fullOnFirstRun = false),
        )

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 0) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { syncJobPersister.tryClaimStaleJob(any()) }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup does nothing when disabled and jobs already exist`() {
        val scheduler = createScheduler(
            testProperties(resumeOnStartup = false, fullOnFirstRun = true),
        )

        every { syncJobFinder.findLatest() } returns TestSyncJobs.sample(status = SyncStatus.COMPLETED)

        scheduler.resumeInterruptedSyncOnStartup()

        verify(exactly = 0) { resumeSyncCommand.execute(any()) }
        verify(exactly = 0) { fullSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick skips when an active job is already running`() {
        val scheduler = createScheduler()

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns true

        scheduler.runSyncTick()

        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `runSyncTick does nothing when first run full sync is disabled`() {
        val scheduler = createScheduler(testProperties(fullOnFirstRun = false))

        every { syncJobPersister.tryClaimStaleJob(any()) } returns null
        every { syncJobFinder.hasActiveJobs() } returns false
        every { resumeSyncCommand.execute(any()) } returns false
        every { syncJobFinder.findByStatusIn(any()) } returns emptyList()
        every { syncJobFinder.findLatest() } returns null

        scheduler.runSyncTick()

        verify(exactly = 0) { fullSyncCommand.execute() }
        verify(exactly = 0) { incrementalSyncCommand.execute() }
    }

    @Test
    fun `onShutdown marks active jobs as FAILED`() {
        val scheduler = createScheduler()

        every { syncJobPersister.failActiveJobs(any()) } returns 1

        scheduler.onShutdown()

        verify(exactly = 1) { syncJobPersister.failActiveJobs("Application shutdown") }
    }

    @Test
    fun `onShutdown does not throw when failActiveJobs fails`() {
        val scheduler = createScheduler()

        every { syncJobPersister.failActiveJobs(any()) } throws RuntimeException("DB gone")

        scheduler.onShutdown()

        verify(exactly = 1) { syncJobPersister.failActiveJobs(any()) }
    }

    @Test
    fun `resumeInterruptedSyncOnStartup releases startup lock when recovery path throws`() {
        val scheduler = createScheduler(testProperties(resumeOnStartup = true))
        val simpleLock = mockk<SimpleLock>(relaxed = true)
        every { lockProvider.lock(any()) } returns Optional.of(simpleLock)
        every { syncJobPersister.tryClaimStaleJob(any()) } throws RuntimeException("DB gone")

        shouldThrow<RuntimeException> {
            scheduler.resumeInterruptedSyncOnStartup()
        }

        verify(exactly = 1) { simpleLock.unlock() }
    }

    private fun testProperties(
        resumeOnStartup: Boolean = true,
        fullOnFirstRun: Boolean = true,
    ) = SyncSchedulerProperties(
        resumeOnStartup = resumeOnStartup,
        fullOnFirstRun = fullOnFirstRun,
        incrementalCron = "0 0 */2 * * *",
        incrementalLockAtMostFor = Duration.ofMinutes(45),
        incrementalLockAtLeastFor = Duration.ofSeconds(10),
        staleJobThreshold = Duration.ofMinutes(10),
    )
}
