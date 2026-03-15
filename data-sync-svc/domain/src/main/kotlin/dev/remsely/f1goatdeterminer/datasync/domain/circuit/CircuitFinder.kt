package dev.remsely.f1goatdeterminer.datasync.domain.circuit

interface CircuitFinder {
    fun findById(id: Int): Circuit?
    fun findByRef(ref: String): Circuit?
    fun findIdByRef(ref: String): Int?
    fun findAllRefToId(): Map<String, Int>
    fun count(): Long
}
