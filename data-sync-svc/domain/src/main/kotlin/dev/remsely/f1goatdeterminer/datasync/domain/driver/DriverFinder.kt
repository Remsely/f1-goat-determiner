package dev.remsely.f1goatdeterminer.datasync.domain.driver

interface DriverFinder {
    fun findById(id: Int): Driver?
    fun count(): Long
}
