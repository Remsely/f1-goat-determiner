package dev.remsely.f1goatdeterminer.datasync.usecase.sync

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SyncEntityTypeDependencyTest {

    @Test
    fun `base entities have no dependencies`() {
        SyncEntityType.STATUSES.dependencies shouldBe emptySet()
        SyncEntityType.CIRCUITS.dependencies shouldBe emptySet()
        SyncEntityType.CONSTRUCTORS.dependencies shouldBe emptySet()
        SyncEntityType.DRIVERS.dependencies shouldBe emptySet()
    }

    @Test
    fun `GRAND_PRIX depends on CIRCUITS`() {
        SyncEntityType.GRAND_PRIX.dependencies shouldContainExactlyInAnyOrder listOf(
            SyncEntityType.CIRCUITS,
        )
    }

    @Test
    fun `RACE_RESULTS depends on GRAND_PRIX, DRIVERS, CONSTRUCTORS, STATUSES`() {
        SyncEntityType.RACE_RESULTS.dependencies shouldContainExactlyInAnyOrder listOf(
            SyncEntityType.GRAND_PRIX,
            SyncEntityType.DRIVERS,
            SyncEntityType.CONSTRUCTORS,
            SyncEntityType.STATUSES,
        )
    }

    @Test
    fun `QUALIFYING_RESULTS depends on GRAND_PRIX, DRIVERS, CONSTRUCTORS`() {
        SyncEntityType.QUALIFYING_RESULTS.dependencies shouldContainExactlyInAnyOrder listOf(
            SyncEntityType.GRAND_PRIX,
            SyncEntityType.DRIVERS,
            SyncEntityType.CONSTRUCTORS,
        )
    }

    @Test
    fun `DRIVER_STANDINGS depends on GRAND_PRIX, DRIVERS`() {
        SyncEntityType.DRIVER_STANDINGS.dependencies shouldContainExactlyInAnyOrder listOf(
            SyncEntityType.GRAND_PRIX,
            SyncEntityType.DRIVERS,
        )
    }

    @Test
    fun `CONSTRUCTOR_STANDINGS depends on GRAND_PRIX, CONSTRUCTORS`() {
        SyncEntityType.CONSTRUCTOR_STANDINGS.dependencies shouldContainExactlyInAnyOrder listOf(
            SyncEntityType.GRAND_PRIX,
            SyncEntityType.CONSTRUCTORS,
        )
    }

    @Test
    fun `syncOrdered contains every entity type exactly once in expected order`() {
        val ordered = SyncEntityType.syncOrdered

        ordered shouldHaveSize enumValues<SyncEntityType>().size
        ordered shouldContainExactly listOf(
            SyncEntityType.STATUSES,
            SyncEntityType.CIRCUITS,
            SyncEntityType.CONSTRUCTORS,
            SyncEntityType.DRIVERS,
            SyncEntityType.GRAND_PRIX,
            SyncEntityType.RACE_RESULTS,
            SyncEntityType.QUALIFYING_RESULTS,
            SyncEntityType.DRIVER_STANDINGS,
            SyncEntityType.CONSTRUCTOR_STANDINGS,
        )
        ordered.distinct() shouldContainExactly ordered
    }

    @Test
    fun `syncOrdered respects dependency ordering`() {
        val ordered = SyncEntityType.syncOrdered
        for (entityType in ordered) {
            val currentIndex = ordered.indexOf(entityType)
            for (dependency in entityType.dependencies) {
                val depIndex = ordered.indexOf(dependency)
                depIndex shouldBeLessThan currentIndex
            }
        }
    }
}
