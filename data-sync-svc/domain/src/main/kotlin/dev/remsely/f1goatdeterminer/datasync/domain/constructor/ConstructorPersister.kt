package dev.remsely.f1goatdeterminer.datasync.domain.constructor

interface ConstructorPersister {
    fun upsertAll(constructors: List<Constructor>): Int
}
