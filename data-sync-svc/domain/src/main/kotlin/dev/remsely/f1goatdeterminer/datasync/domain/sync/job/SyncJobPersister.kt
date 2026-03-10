package dev.remsely.f1goatdeterminer.datasync.domain.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import java.time.LocalDateTime

interface SyncJobPersister {
    fun save(job: SyncJob): SyncJob

    /**
     * Atomically checks that no active jobs exist and creates a new one.
     * Uses an advisory lock to prevent race conditions between instances.
     *
     * @return the created job, or null if an active job already exists
     */
    fun tryCreateJob(type: SyncJob.Type): SyncJob?

    fun updateStatus(id: Long, status: SyncStatus, errorMessage: String? = null)
    fun updateProgress(id: Long, totalRequests: Int, failedRequests: Int)

    /**
     * Touches the job's `updated_at` timestamp to signal liveness.
     * Called during checkpoint progress updates to prevent stale detection
     * for long-running checkpoints (e.g., RACE_RESULTS ~17 min).
     */
    fun touchUpdatedAt(id: Long)
    fun complete(id: Long, status: SyncStatus, completedAt: LocalDateTime = LocalDateTime.now())

    /**
     * Atomically finds a resumable job (FAILED or PAUSED) and claims it
     * by setting its status to PENDING. Returns the claimed job, or null if none found.
     *
     * Only claims jobs whose `updated_at` is older than [updatedBefore], providing
     * a cooldown period after failures (e.g., rate limit exhaustion) before retrying.
     *
     * Does NOT claim IN_PROGRESS jobs — those may be actively running on another instance.
     * Use [tryClaimStaleJob] for stuck IN_PROGRESS jobs.
     */
    fun tryClaimResumableJob(updatedBefore: LocalDateTime): SyncJob?

    /**
     * Atomically finds a stale job (IN_PROGRESS or PENDING with [updatedBefore] threshold)
     * and claims it by setting its status to PENDING.
     *
     * A stale job is one whose `updated_at` is older than [updatedBefore], indicating
     * the owning instance likely died without completing the job.
     */
    fun tryClaimStaleJob(updatedBefore: LocalDateTime): SyncJob?

    /**
     * Marks all active jobs (IN_PROGRESS, PENDING) as FAILED.
     * Used during graceful shutdown to ensure jobs are resumable on next startup.
     *
     * @return number of jobs marked as failed
     */
    fun failActiveJobs(reason: String): Int
}
