package dev.remsely.f1goatdeterminer.datasync.domain.constructor

interface ConstructorFinder {
    fun findById(id: Int): Constructor?
    fun findByRef(ref: String): Constructor?
    fun findAll(): List<Constructor>
    fun count(): Long
}
