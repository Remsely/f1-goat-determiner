package dev.remsely.f1goatdeterminer.datasync.domain.circuit

interface CircuitPersister {
    fun upsertAll(circuits: List<Circuit>): Int
}
