package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

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

        return jdbcTemplate.batchUpdate(sql, standings, standings.size) { ps, s ->
            ps.setInt(1, s.grandPrixId)
            ps.setInt(2, s.constructorId)
            ps.setBigDecimal(3, s.points)
            ps.setInt(4, s.position)
            ps.setString(5, s.positionText)
            ps.setInt(6, s.wins)
        }.sumOf { it.sum() }
    }
}
