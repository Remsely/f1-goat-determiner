package dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Time
import java.sql.Types

@Component
class GrandPrixJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(grandPrixList: List<GrandPrix>): Int {
        if (grandPrixList.isEmpty()) return 0

        val sql = """
            INSERT INTO races (season, round, circuit_id, name, date, time)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (season, round) DO UPDATE SET
                circuit_id = EXCLUDED.circuit_id,
                name = EXCLUDED.name,
                date = EXCLUDED.date,
                time = EXCLUDED.time
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, grandPrixList, grandPrixList.size) { ps, gp ->
            val time = gp.time

            ps.setInt(1, gp.season)
            ps.setInt(2, gp.round)
            ps.setInt(3, gp.circuitId)
            ps.setString(4, gp.name)
            ps.setDate(5, Date.valueOf(gp.date))

            if (time != null) ps.setTime(6, Time.valueOf(time)) else ps.setNull(6, Types.TIME)
        }.sumOf { it.sum() }
    }
}
