package dev.remsely.f1goatdeterminer.datasync.db.repository.circuit

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component

@Component
class CircuitJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(circuits: List<Circuit>): Int {
        if (circuits.isEmpty()) return 0

        val sql = """
            INSERT INTO circuits (ref, name, locality, country)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (ref) DO UPDATE SET
                name = EXCLUDED.name,
                locality = EXCLUDED.locality,
                country = EXCLUDED.country
        """.trimIndent()

        val countBefore = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM circuits")!!

        jdbcTemplate.batchUpdate(sql, circuits, circuits.size) { ps, circuit ->
            ps.setString(1, circuit.ref)
            ps.setString(2, circuit.name)
            ps.setString(3, circuit.locality)
            ps.setString(4, circuit.country)
        }

        val countAfter = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM circuits")!!
        return (countAfter - countBefore).toInt()
    }
}
