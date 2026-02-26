package dev.remsely.f1goatdeterminer.datasync.domain.driver

interface DriverPersister {
    fun upsertAll(drivers: List<Driver>): Int
}
