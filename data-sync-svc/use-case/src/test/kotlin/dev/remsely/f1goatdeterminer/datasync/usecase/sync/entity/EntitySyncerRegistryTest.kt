package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntitySyncerRegistryTest {

    private fun fakeSyncer(type: SyncEntityType, recordsSynced: Int = 0) = object : EntitySyncer {
        override val entityType: SyncEntityType = type
        override fun sync(checkpoint: SyncCheckpoint): SyncResult =
            SyncResult(recordsSynced, recordsSynced, null, null, 0)
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
    fun `getSyncer throws exact error for unregistered entity type`() {
        val registry = EntitySyncerRegistry(emptyList())

        val error = shouldThrow<IllegalStateException> {
            registry.getSyncer(SyncEntityType.STATUSES)
        }

        error.message shouldBe "No EntitySyncer registered for entity type: STATUSES"
    }

    @Test
    fun `registry keeps last syncer when duplicate entity type is registered`() {
        val first = fakeSyncer(SyncEntityType.STATUSES, recordsSynced = 1)
        val second = fakeSyncer(SyncEntityType.STATUSES, recordsSynced = 2)
        val registry = EntitySyncerRegistry(listOf(first, second))

        registry.getSyncer(SyncEntityType.STATUSES) shouldBe second
    }
}
