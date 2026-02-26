package dev.remsely.f1goatdeterminer.datasync.domain.circuit

interface CircuitFinder {
    fun findById(id: Int): Circuit?
    fun count(): Long
}
