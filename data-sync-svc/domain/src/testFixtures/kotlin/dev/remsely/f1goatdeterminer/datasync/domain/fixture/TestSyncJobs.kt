package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import java.time.LocalDateTime

object TestSyncJobs {
    private val DEFAULT_TIME: LocalDateTime = LocalDateTime.of(2024, 6, 1, 12, 0)

    fun sample(
        id: Long? = 1L,
        type: SyncJob.Type = SyncJob.Type.FULL,
        status: SyncStatus = SyncStatus.PENDING,
        startedAt: LocalDateTime = DEFAULT_TIME,
        updatedAt: LocalDateTime = startedAt,
        completedAt: LocalDateTime? = null,
        errorMessage: String? = null,
        totalRequests: Int = 0,
        failedRequests: Int = 0,
    ) = SyncJob(
        id = id,
        type = type,
        status = status,
        startedAt = startedAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        errorMessage = errorMessage,
        totalRequests = totalRequests,
        failedRequests = failedRequests,
    )
}

