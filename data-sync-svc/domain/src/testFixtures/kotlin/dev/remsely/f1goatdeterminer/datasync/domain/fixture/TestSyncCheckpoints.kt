package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import java.time.LocalDateTime

object TestSyncCheckpoints {
    private val DEFAULT_TIME: LocalDateTime = LocalDateTime.of(2024, 6, 1, 12, 0)

    fun completed(
        jobId: Long,
        entityType: SyncEntityType,
        lastOffset: Int,
        lastSeason: Int? = null,
        lastRound: Int? = null,
        recordsSynced: Int = 1000,
    ) = SyncCheckpoint(
        id = null,
        jobId = jobId,
        entityType = entityType,
        status = SyncStatus.COMPLETED,
        lastOffset = lastOffset,
        lastSeason = lastSeason,
        lastRound = lastRound,
        recordsSynced = recordsSynced,
        startedAt = DEFAULT_TIME,
        completedAt = DEFAULT_TIME.plusMinutes(5),
        errorMessage = null,
        retryCount = 0,
    )
}

