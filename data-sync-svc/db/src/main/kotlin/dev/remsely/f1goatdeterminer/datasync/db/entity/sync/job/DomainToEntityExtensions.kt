package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob

fun SyncJob.toEntity() = SyncJobEntity(
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

fun List<SyncJob>.toEntity(): List<SyncJobEntity> = map { it.toEntity() }
