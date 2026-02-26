package dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import java.time.LocalDateTime

interface SyncCheckpointPersister {
    fun save(checkpoint: SyncCheckpoint): SyncCheckpoint

    fun saveAll(checkpoints: List<SyncCheckpoint>): List<SyncCheckpoint>

    fun updateStatus(id: Long, status: SyncStatus, errorMessage: String? = null)

    fun updateProgress(
        id: Long,
        lastOffset: Int? = null,
        lastSeason: Int? = null,
        lastRound: Int? = null,
        recordsSynced: Int,
    )

    fun incrementRetryCount(id: Long)

    fun complete(id: Long, recordsSynced: Int, completedAt: LocalDateTime = LocalDateTime.now())

    fun fail(id: Long, errorMessage: String)
}
