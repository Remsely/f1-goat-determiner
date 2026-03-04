package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint

/**
 * Interface for entity-specific synchronization logic.
 * Each implementation handles fetching data from an external source and persisting it to the database
 * for a specific [SyncEntityType].
 */
interface EntitySyncer {
    val entityType: SyncEntityType

    /**
     * Synchronizes data for this entity type, resuming from the given checkpoint state.
     *
     * @param checkpoint the current checkpoint to resume from
     * @return result of the sync operation
     */
    fun sync(checkpoint: SyncCheckpoint): SyncResult
}

/**
 * Result of an entity sync operation.
 */
data class SyncResult(
    val recordsSynced: Int,
    val lastOffset: Int,
    val lastSeason: Int?,
    val lastRound: Int?,
    val apiCallsMade: Int,
)
