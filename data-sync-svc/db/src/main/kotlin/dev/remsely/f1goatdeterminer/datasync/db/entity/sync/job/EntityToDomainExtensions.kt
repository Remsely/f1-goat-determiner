package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.job

import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob

fun SyncJobEntity.toDomain() = SyncJob(
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

fun List<SyncJobEntity>.toDomain(): List<SyncJob> = map { it.toDomain() }
