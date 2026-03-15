package dev.remsely.f1goatdeterminer.datasync.db.repository.circuit

import dev.remsely.f1goatdeterminer.datasync.db.entity.circuit.CircuitEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CircuitJpaRepository : JpaRepository<CircuitEntity, Int> {
    fun findByRef(ref: String): CircuitEntity?
}
