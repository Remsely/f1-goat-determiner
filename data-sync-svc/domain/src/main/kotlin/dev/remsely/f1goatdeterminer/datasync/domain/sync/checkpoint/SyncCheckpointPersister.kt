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

    /**
     * Resets a FAILED checkpoint so it can be retried on next resume.
     * Sets status to FAILED with retry_count=0 so [SyncCheckpoint.isRetryable] returns true.
     * Progress (lastOffset/lastSeason/lastRound/recordsSynced) is preserved.
     */
    fun resetRetryCount(id: Long)

    fun completeWithProgress(
        id: Long,
        lastOffset: Int?,
        lastSeason: Int?,
        lastRound: Int?,
        recordsSynced: Int,
        completedAt: LocalDateTime = LocalDateTime.now(),
    )

    fun fail(id: Long, errorMessage: String)
}
