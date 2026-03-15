package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class ConstructorStandingJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(standings: List<ConstructorStanding>): Int {
        if (standings.isEmpty()) return 0

        val sql = """
            INSERT INTO constructor_standings (race_id, constructor_id, points, position, position_text, wins)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (race_id, constructor_id) DO UPDATE SET
                points = EXCLUDED.points,
                position = EXCLUDED.position,
                position_text = EXCLUDED.position_text,
                wins = EXCLUDED.wins
        """.trimIndent()

        val countBefore = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM constructor_standings")!!

        jdbcTemplate.batchUpdate(sql, standings, standings.size) { ps, s ->
            val position = s.position

            ps.setInt(1, s.grandPrixId)
            ps.setInt(2, s.constructorId)
            ps.setBigDecimal(3, s.points)
            if (position != null) ps.setInt(4, position) else ps.setNull(4, Types.INTEGER)
            ps.setString(5, s.positionText)
            ps.setInt(6, s.wins)
        }

        val countAfter = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM constructor_standings")!!
        return (countAfter - countBefore).toInt()
    }
}
