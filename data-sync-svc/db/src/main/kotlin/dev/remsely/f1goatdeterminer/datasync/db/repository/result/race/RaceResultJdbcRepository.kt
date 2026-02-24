package dev.remsely.f1goatdeterminer.datasync.db.repository.result.race

import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class RaceResultJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(results: List<RaceResult>): Int {
        if (results.isEmpty()) return 0

        val sql = """
            INSERT INTO results (
                id, race_id, driver_id, constructor_id, number, grid,
                position, position_text, position_order, points, laps,
                time, milliseconds, fastest_lap, fastest_lap_rank,
                fastest_lap_time, fastest_lap_speed, status_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                race_id = EXCLUDED.race_id,
                driver_id = EXCLUDED.driver_id,
                constructor_id = EXCLUDED.constructor_id,
                number = EXCLUDED.number,
                grid = EXCLUDED.grid,
                position = EXCLUDED.position,
                position_text = EXCLUDED.position_text,
                position_order = EXCLUDED.position_order,
                points = EXCLUDED.points,
                laps = EXCLUDED.laps,
                time = EXCLUDED.time,
                milliseconds = EXCLUDED.milliseconds,
                fastest_lap = EXCLUDED.fastest_lap,
                fastest_lap_rank = EXCLUDED.fastest_lap_rank,
                fastest_lap_time = EXCLUDED.fastest_lap_time,
                fastest_lap_speed = EXCLUDED.fastest_lap_speed,
                status_id = EXCLUDED.status_id
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, results, results.size) { ps, r ->
            val number = r.number
            val position = r.position
            val milliseconds = r.milliseconds
            val fastestLap = r.fastestLap
            val fastestLapRank = r.fastestLapRank
            val fastestLapSpeed = r.fastestLapSpeed

            ps.setInt(1, r.id)
            ps.setInt(2, r.grandPrixId)
            ps.setInt(3, r.driverId)
            ps.setInt(4, r.constructorId)

            if (number != null) ps.setInt(5, number) else ps.setNull(5, Types.INTEGER)

            ps.setInt(6, r.grid)

            if (position != null) ps.setInt(7, position) else ps.setNull(7, Types.INTEGER)

            ps.setString(8, r.positionText)
            ps.setInt(9, r.positionOrder)
            ps.setBigDecimal(10, r.points)
            ps.setInt(11, r.laps)
            ps.setString(12, r.time)

            if (milliseconds != null) ps.setLong(13, milliseconds) else ps.setNull(13, Types.BIGINT)
            if (fastestLap != null) ps.setInt(14, fastestLap) else ps.setNull(14, Types.INTEGER)
            if (fastestLapRank != null) ps.setInt(15, fastestLapRank) else ps.setNull(15, Types.INTEGER)

            ps.setString(16, r.fastestLapTime)

            if (fastestLapSpeed != null) ps.setBigDecimal(17, fastestLapSpeed) else ps.setNull(17, Types.DECIMAL)

            ps.setInt(18, r.statusId)
        }.sumOf { it.sum() }
    }
}
