package dev.remsely.f1goatdeterminer.datasync.db.repository.circuit

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class CircuitJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(circuits: List<Circuit>): Int {
        if (circuits.isEmpty()) return 0

        val sql = """
            INSERT INTO circuits (id, ref, name, locality, country)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                ref = EXCLUDED.ref,
                name = EXCLUDED.name,
                locality = EXCLUDED.locality,
                country = EXCLUDED.country
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, circuits, circuits.size) { ps, circuit ->
            ps.setInt(1, circuit.id)
            ps.setString(2, circuit.ref)
            ps.setString(3, circuit.name)
            ps.setString(4, circuit.locality)
            ps.setString(5, circuit.country)
        }.sumOf { it.sum() }
    }
}
