package dev.remsely.f1goatdeterminer.datasync.db.repository.result.qualifying

import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class QualifyingResultJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(results: List<QualifyingResult>): Int {
        if (results.isEmpty()) return 0

        val sql = """
            INSERT INTO qualifying (race_id, driver_id, constructor_id, number, position, q1, q2, q3)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (race_id, driver_id) DO UPDATE SET
                constructor_id = EXCLUDED.constructor_id,
                number = EXCLUDED.number,
                position = EXCLUDED.position,
                q1 = EXCLUDED.q1,
                q2 = EXCLUDED.q2,
                q3 = EXCLUDED.q3
        """.trimIndent()

        val countBefore = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM qualifying")!!

        jdbcTemplate.batchUpdate(sql, results, results.size) { ps, q ->
            val number = q.number

            ps.setInt(1, q.grandPrixId)
            ps.setInt(2, q.driverId)
            ps.setInt(3, q.constructorId)

            if (number != null) ps.setInt(4, number) else ps.setNull(4, Types.INTEGER)

            ps.setInt(5, q.position)
            ps.setString(6, q.q1)
            ps.setString(7, q.q2)
            ps.setString(8, q.q3)
        }

        val countAfter = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM qualifying")!!
        return (countAfter - countBefore).toInt()
    }
}
