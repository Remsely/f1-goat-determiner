package dev.remsely.f1goatdeterminer.datasync.db.repository.result.qualifying

import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class QualifyingResultJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(results: List<QualifyingResult>): Int {
        if (results.isEmpty()) return 0

        val sql = """
            INSERT INTO qualifying (id, race_id, driver_id, constructor_id, number, position, q1, q2, q3)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                race_id = EXCLUDED.race_id,
                driver_id = EXCLUDED.driver_id,
                constructor_id = EXCLUDED.constructor_id,
                number = EXCLUDED.number,
                position = EXCLUDED.position,
                q1 = EXCLUDED.q1,
                q2 = EXCLUDED.q2,
                q3 = EXCLUDED.q3
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, results, results.size) { ps, q ->
            val number = q.number

            ps.setInt(1, q.id)
            ps.setInt(2, q.grandPrixId)
            ps.setInt(3, q.driverId)
            ps.setInt(4, q.constructorId)

            if (number != null) ps.setInt(5, number) else ps.setNull(5, Types.INTEGER)

            ps.setInt(6, q.position)
            ps.setString(7, q.q1)
            ps.setString(8, q.q2)
            ps.setString(9, q.q3)
        }.sumOf { it.sum() }
    }
}
