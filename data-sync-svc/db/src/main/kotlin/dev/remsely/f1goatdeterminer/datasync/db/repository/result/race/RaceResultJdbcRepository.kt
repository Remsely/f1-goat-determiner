package dev.remsely.f1goatdeterminer.datasync.db.repository.result.race

import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class RaceResultJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    @Suppress("LongMethod")
    fun upsertAll(results: List<RaceResult>): Int {
        if (results.isEmpty()) return 0

        val sql = """
            INSERT INTO results (
                race_id, driver_id, constructor_id, number, grid,
                position, position_text, position_order, points, laps,
                time, milliseconds, fastest_lap, fastest_lap_rank,
                fastest_lap_time, fastest_lap_speed, status_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (race_id, driver_id) DO UPDATE SET
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

            ps.setInt(1, r.grandPrixId)
            ps.setInt(2, r.driverId)
            ps.setInt(3, r.constructorId)

            if (number != null) ps.setInt(4, number) else ps.setNull(4, Types.INTEGER)

            ps.setInt(5, r.grid)

            if (position != null) ps.setInt(6, position) else ps.setNull(6, Types.INTEGER)

            ps.setString(7, r.positionText)
            ps.setInt(8, r.positionOrder)
            ps.setBigDecimal(9, r.points)
            ps.setInt(10, r.laps)
            ps.setString(11, r.time)

            if (milliseconds != null) ps.setLong(12, milliseconds) else ps.setNull(12, Types.BIGINT)
            if (fastestLap != null) ps.setInt(13, fastestLap) else ps.setNull(13, Types.INTEGER)
            if (fastestLapRank != null) ps.setInt(14, fastestLapRank) else ps.setNull(14, Types.INTEGER)

            ps.setString(15, r.fastestLapTime)

            if (fastestLapSpeed != null) ps.setBigDecimal(16, fastestLapSpeed) else ps.setNull(16, Types.DECIMAL)

            ps.setInt(17, r.statusId)
        }.sumOf { it.sum() }
    }
}
