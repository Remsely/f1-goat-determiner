package dev.remsely.f1goatdeterminer.datasync.domain.constructor

interface ConstructorFinder {
    fun findById(id: Int): Constructor?
    fun findByRef(ref: String): Constructor?
    fun findIdByRef(ref: String): Int?
    fun findAllRefToId(): Map<String, Int>
    fun count(): Long
}
