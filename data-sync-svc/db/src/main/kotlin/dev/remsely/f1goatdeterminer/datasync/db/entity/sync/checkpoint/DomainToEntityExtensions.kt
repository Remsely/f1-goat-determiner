package dev.remsely.f1goatdeterminer.datasync.db.entity.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint

fun SyncCheckpoint.toEntity() = SyncCheckpointEntity(
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


fun List<SyncCheckpoint>.toEntity(): List<SyncCheckpointEntity> = map { it.toEntity() }
