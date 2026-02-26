package dev.remsely.f1goatdeterminer.datasync.db.repository.circuit

import dev.remsely.f1goatdeterminer.datasync.db.entity.circuit.toDomain
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitFinder
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitPersister
import org.springframework.stereotype.Service

@Service
class CircuitDao(
    private val jpaRepository: CircuitJpaRepository,
    private val jdbcRepository: CircuitJdbcRepository,
) : CircuitFinder, CircuitPersister {

    override fun findById(id: Int): Circuit? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun count(): Long = jpaRepository.count()

    override fun upsertAll(circuits: List<Circuit>): Int = jdbcRepository.upsertAll(circuits)
}
