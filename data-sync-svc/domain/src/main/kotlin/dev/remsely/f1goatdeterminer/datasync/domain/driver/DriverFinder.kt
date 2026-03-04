package dev.remsely.f1goatdeterminer.datasync.domain.driver

interface DriverFinder {
    fun findById(id: Int): Driver?
    fun findByRef(ref: String): Driver?
    fun findIdByRef(ref: String): Int?
    fun findAllRefToId(): Map<String, Int>
    fun count(): Long
}
