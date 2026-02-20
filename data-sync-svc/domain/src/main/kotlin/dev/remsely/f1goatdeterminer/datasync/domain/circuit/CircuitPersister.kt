package dev.remsely.f1goatdeterminer.datasync.domain.circuit

interface CircuitPersister {
    fun save(circuit: Circuit): Circuit
    fun saveAll(circuits: List<Circuit>): List<Circuit>
    fun upsertAll(circuits: List<Circuit>): Int
}
