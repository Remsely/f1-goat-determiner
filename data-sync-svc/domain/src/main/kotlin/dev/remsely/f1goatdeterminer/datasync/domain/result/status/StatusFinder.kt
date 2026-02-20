package dev.remsely.f1goatdeterminer.datasync.domain.result.status

interface StatusFinder {
    fun findById(id: Int): Status?
    fun findAll(): List<Status>
    fun count(): Long
}
