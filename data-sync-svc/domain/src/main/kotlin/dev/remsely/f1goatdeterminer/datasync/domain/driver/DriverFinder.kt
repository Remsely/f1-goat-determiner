package dev.remsely.f1goatdeterminer.datasync.domain.driver

interface DriverFinder {
    fun findById(id: Int): Driver?
    fun findByRef(ref: String): Driver?
    fun findByNationality(nationality: String): List<Driver>
    fun findAll(): List<Driver>
    fun count(): Long
}
