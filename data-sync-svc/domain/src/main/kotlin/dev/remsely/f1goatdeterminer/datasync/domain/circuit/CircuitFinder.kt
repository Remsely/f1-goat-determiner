package dev.remsely.f1goatdeterminer.datasync.domain.circuit

interface CircuitFinder {
    fun findById(id: Int): Circuit?
    fun findByRef(ref: String): Circuit?
    fun findAll(): List<Circuit>
    fun count(): Long
}
