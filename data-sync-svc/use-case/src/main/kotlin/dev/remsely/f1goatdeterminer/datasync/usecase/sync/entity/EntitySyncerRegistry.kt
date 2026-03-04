package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import org.springframework.stereotype.Component

/**
 * Registry that maps [SyncEntityType] to its corresponding [EntitySyncer] implementation.
 */
@Component
class EntitySyncerRegistry(syncers: List<EntitySyncer>) {

    private val syncerMap: Map<SyncEntityType, EntitySyncer> =
        syncers.associateBy { it.entityType }

    fun getSyncer(entityType: SyncEntityType): EntitySyncer =
        syncerMap[entityType]
            ?: error("No EntitySyncer registered for entity type: $entityType")
}
