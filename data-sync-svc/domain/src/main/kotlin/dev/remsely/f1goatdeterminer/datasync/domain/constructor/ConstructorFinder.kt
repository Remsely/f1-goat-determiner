package dev.remsely.f1goatdeterminer.datasync.domain.constructor

interface ConstructorFinder {
    fun findById(id: Int): Constructor?
    fun count(): Long
}
