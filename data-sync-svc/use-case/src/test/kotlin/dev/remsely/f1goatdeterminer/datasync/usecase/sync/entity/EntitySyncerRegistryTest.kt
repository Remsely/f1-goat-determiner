package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntitySyncerRegistryTest {

    private fun fakeSyncer(type: SyncEntityType) = object : EntitySyncer {
        override val entityType: SyncEntityType = type
        override fun sync(checkpoint: SyncCheckpoint): SyncResult =
            SyncResult(0, 0, null, null, 0)
    }

    @Test
    fun `getSyncer returns correct syncer for entity type`() {
        val statusSyncer = fakeSyncer(SyncEntityType.STATUSES)
        val circuitSyncer = fakeSyncer(SyncEntityType.CIRCUITS)
        val registry = EntitySyncerRegistry(listOf(statusSyncer, circuitSyncer))

        registry.getSyncer(SyncEntityType.STATUSES) shouldBe statusSyncer
        registry.getSyncer(SyncEntityType.CIRCUITS) shouldBe circuitSyncer
    }

    @Test
    fun `getSyncer throws for unregistered entity type`() {
        val registry = EntitySyncerRegistry(emptyList())

        shouldThrow<IllegalStateException> {
            registry.getSyncer(SyncEntityType.STATUSES)
        }
    }
}
