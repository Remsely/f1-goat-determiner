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
