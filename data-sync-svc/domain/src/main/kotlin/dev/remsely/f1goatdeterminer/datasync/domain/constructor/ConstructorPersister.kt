package dev.remsely.f1goatdeterminer.datasync.domain.constructor

interface ConstructorPersister {
    fun save(constructor: Constructor): Constructor
    fun saveAll(constructors: List<Constructor>): List<Constructor>
    fun upsertAll(constructors: List<Constructor>): Int
}
