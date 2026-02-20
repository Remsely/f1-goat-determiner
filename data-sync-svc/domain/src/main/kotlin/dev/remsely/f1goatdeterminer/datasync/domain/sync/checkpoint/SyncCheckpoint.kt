package dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import java.time.LocalDateTime

data class SyncCheckpoint(
    val id: Long?,
    val jobId: Long,
    val entityType: SyncEntityType,
    val status: SyncStatus,
    val lastOffset: Int,
    val lastSeason: Int?,
    val lastRound: Int?,
    val recordsSynced: Int,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val errorMessage: String?,
    val retryCount: Int,
) {
    val isCompleted: Boolean
        get() = status == SyncStatus.COMPLETED

    val isPending: Boolean
        get() = status == SyncStatus.PENDING

    val isRetryable: Boolean
        get() = status == SyncStatus.FAILED && retryCount < MAX_RETRIES

    companion object {
        const val MAX_RETRIES = 3

        fun initPending(jobId: Long, entityType: SyncEntityType) = SyncCheckpoint(
            id = null,
            jobId = jobId,
            entityType = entityType,
            status = SyncStatus.PENDING,
            lastOffset = 0,
            lastSeason = null,
            lastRound = null,
            recordsSynced = 0,
            startedAt = null,
            completedAt = null,
            errorMessage = null,
            retryCount = 0,
        )
    }
}
