package dev.remsely.f1goatdeterminer.datasync.scheduled

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJobPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.command.FullSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.IncrementalSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.command.ResumeSyncCommand
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.SyncOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

private val STARTUP_LOCK_AT_MOST = Duration.ofMinutes(45)
private val STARTUP_LOCK_AT_LEAST = Duration.ofSeconds(5)

@Component
class SyncScheduler(
    private val fullSyncCommand: FullSyncCommand,
    private val incrementalSyncCommand: IncrementalSyncCommand,
    private val resumeSyncCommand: ResumeSyncCommand,
    private val syncOrchestrator: SyncOrchestrator,
    private val syncJobFinder: SyncJobFinder,
    private val syncJobPersister: SyncJobPersister,
    private val lockProvider: LockProvider,
    private val properties: SyncSchedulerProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun resumeInterruptedSyncOnStartup() {
        val lockConfig = LockConfiguration(
            Instant.now(),
            "syncScheduler.startup",
            STARTUP_LOCK_AT_MOST,
            STARTUP_LOCK_AT_LEAST,
        )
        val lock = lockProvider.lock(lockConfig)
        if (lock.isEmpty) {
            log.info { "-- Startup resume skipped: another instance holds the startup lock" }
            return
        }

        val simpleLock = lock.get()
        try {
            doResumeInterruptedSyncOnStartup()
        } finally {
            simpleLock.unlock()
        }
    }

    private fun doResumeInterruptedSyncOnStartup() {
        if (properties.resumeOnStartup) {
            if (tryRecoverStaleJob()) return
            if (resumeSyncCommand.execute(cooldown = properties.staleJobThreshold)) {
                log.info { "-- Finished startup resume of interrupted sync job" }
                return
            }
            log.info { "-- No interrupted sync jobs to resume on startup" }
        } else {
            log.info { "-- Startup resume is disabled by configuration" }
        }

        if (properties.fullOnFirstRun && syncJobFinder.findLatest() == null) {
            log.info { ">> No previous sync jobs found, running initial full sync on startup" }
            fullSyncCommand.execute()
        }
    }

    private fun tryRecoverStaleJob(): Boolean {
        val staleBefore = LocalDateTime.now().minus(properties.staleJobThreshold)
        val staleJob = syncJobPersister.tryClaimStaleJob(staleBefore) ?: return false

        val jobId = requireNotNull(staleJob.id)
        log.warn {
            "!! Recovered stale sync job #$jobId (was stuck, updated_at < $staleBefore)"
        }
        syncOrchestrator.execute(staleJob)
        return true
    }

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        val failed = runCatching {
            syncJobPersister.failActiveJobs("Application shutdown")
        }.getOrElse { e ->
            log.warn(e) { "!! Failed to mark active jobs on shutdown: ${e.message}" }
            0
        }
        if (failed > 0) {
            log.info { "-- Shutdown: marked $failed active job(s) as FAILED for resume on next startup" }
        }
    }

    @Scheduled(cron = "\${sync.schedule.incremental-cron}")
    @SchedulerLock(
        name = "syncScheduler.syncTick",
        lockAtMostFor = "\${sync.schedule.incremental-lock-at-most-for}",
        lockAtLeastFor = "\${sync.schedule.incremental-lock-at-least-for}",
    )
    fun runSyncTick() {
        if (tryRecoverStaleJob()) return

        if (syncJobFinder.hasActiveJobs()) {
            log.debug { "-- Sync tick skipped: an active job is already running" }
            return
        }

        if (tryResumeOrWaitForCooldown()) return

        runIncrementalOrFullSync()
    }

    /**
     * Tries to resume a failed/paused job. If resume was blocked by cooldown,
     * skips the tick to avoid creating a duplicate job.
     *
     * @return true if the tick should be skipped (either resumed or waiting for cooldown)
     */
    private fun tryResumeOrWaitForCooldown(): Boolean {
        if (resumeSyncCommand.execute(cooldown = properties.staleJobThreshold)) {
            log.info { "-- Sync tick: resumed a failed/paused job" }
            return true
        }

        val pendingResume = syncJobFinder.findByStatusIn(listOf(SyncStatus.FAILED, SyncStatus.PAUSED))
        if (pendingResume.isNotEmpty()) {
            log.info {
                "-- Sync tick skipped: failed job(s) in cooldown, waiting before retry " +
                    "(job #${pendingResume.first().id})"
            }
            return true
        }

        return false
    }

    private fun runIncrementalOrFullSync() {
        val latestSyncJob = syncJobFinder.findLatest()

        if (latestSyncJob == null) {
            if (properties.fullOnFirstRun) {
                log.info { ">> No previous sync jobs found, running full sync" }
                fullSyncCommand.execute()
            } else {
                log.debug { "-- Sync tick skipped: no previous jobs and full-on-first-run disabled" }
            }
            return
        }

        if (!syncJobFinder.hasCompletedFullSync()) {
            log.warn {
                "-- Sync tick skipped: no completed full sync yet " +
                    "(latest job #${latestSyncJob.id} is ${latestSyncJob.status})"
            }
            return
        }

        log.info { ">> Sync tick: running incremental sync" }
        incrementalSyncCommand.execute()
    }
}
