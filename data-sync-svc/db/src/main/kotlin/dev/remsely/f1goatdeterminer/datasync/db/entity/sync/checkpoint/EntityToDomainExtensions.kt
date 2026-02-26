package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint

fun SyncCheckpointEntity.toDomain() = SyncCheckpoint(
    id = id,
    jobId = jobId,
    entityType = entityType,
    status = status,
    lastOffset = lastOffset,
    lastSeason = lastSeason,
    lastRound = lastRound,
    recordsSynced = recordsSynced,
    startedAt = startedAt,
    completedAt = completedAt,
    errorMessage = errorMessage,
    retryCount = retryCount,
)

fun List<SyncCheckpointEntity>.toDomain(): List<SyncCheckpoint> = map { it.toDomain() }
