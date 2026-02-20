package dev.remsely.f1goatdeterminer.datasync.domain.driver

interface DriverPersister {
    fun save(driver: Driver): Driver
    fun saveAll(drivers: List<Driver>): List<Driver>
    fun upsertAll(drivers: List<Driver>): Int
}
