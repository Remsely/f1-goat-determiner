package dev.remsely.f1goatdeterminer.datasync.domain.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import java.time.LocalDateTime

data class SyncJob(
    val id: Long?,
    val type: Type,
    val status: SyncStatus,
    val startedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val errorMessage: String?,
    val totalRequests: Int,
    val failedRequests: Int,
) {
    enum class Type {
        FULL,
        INCREMENTAL
    }

    val isActive: Boolean
        get() = status == SyncStatus.IN_PROGRESS || status == SyncStatus.PENDING

    val isResumable: Boolean
        get() = status == SyncStatus.PAUSED || status == SyncStatus.FAILED
}
