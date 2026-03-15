package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.driver

import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStanding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class DriverStandingJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(standings: List<DriverStanding>): Int {
        if (standings.isEmpty()) return 0

        val sql = """
            INSERT INTO driver_standings (race_id, driver_id, points, position, position_text, wins)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (race_id, driver_id) DO UPDATE SET
                points = EXCLUDED.points,
                position = EXCLUDED.position,
                position_text = EXCLUDED.position_text,
                wins = EXCLUDED.wins
        """.trimIndent()

        val countBefore = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM driver_standings")!!

        jdbcTemplate.batchUpdate(sql, standings, standings.size) { ps, s ->
            val position = s.position

            ps.setInt(1, s.grandPrixId)
            ps.setInt(2, s.driverId)
            ps.setBigDecimal(3, s.points)
            if (position != null) ps.setInt(4, position) else ps.setNull(4, Types.INTEGER)
            ps.setString(5, s.positionText)
            ps.setInt(6, s.wins)
        }

        val countAfter = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM driver_standings")!!
        return (countAfter - countBefore).toInt()
    }
}
