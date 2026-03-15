package dev.remsely.f1goatdeterminer.datasync.domain.sync

enum class SyncEntityType {
    STATUSES,
    CIRCUITS,
    CONSTRUCTORS,
    DRIVERS,
    GRAND_PRIX,
    RACE_RESULTS,
    QUALIFYING_RESULTS,
    DRIVER_STANDINGS,
    CONSTRUCTOR_STANDINGS,
    ;

    /**
     * Entity types that must be completed before this one can run.
     * If any dependency has FAILED, this entity type should be skipped.
     */
    val dependencies: Set<SyncEntityType>
        get() = when (this) {
            STATUSES -> emptySet()
            CIRCUITS -> emptySet()
            CONSTRUCTORS -> emptySet()
            DRIVERS -> emptySet()
            GRAND_PRIX -> setOf(CIRCUITS)
            RACE_RESULTS -> setOf(GRAND_PRIX, DRIVERS, CONSTRUCTORS, STATUSES)
            QUALIFYING_RESULTS -> setOf(GRAND_PRIX, DRIVERS, CONSTRUCTORS)
            DRIVER_STANDINGS -> setOf(GRAND_PRIX, DRIVERS)
            CONSTRUCTOR_STANDINGS -> setOf(GRAND_PRIX, CONSTRUCTORS)
        }

    companion object {
        /**
         * Returns entity types in the order they should be synced (respecting foreign key dependencies).
         */
        val syncOrdered: List<SyncEntityType> = listOf(
            STATUSES,
            CIRCUITS,
            CONSTRUCTORS,
            DRIVERS,
            GRAND_PRIX,
            RACE_RESULTS,
            QUALIFYING_RESULTS,
            DRIVER_STANDINGS,
            CONSTRUCTOR_STANDINGS,
        )
    }
}
