package dev.remsely.f1goatdeterminer.datasync.db.repository.result.status

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class StatusJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(statuses: List<Status>): Int {
        if (statuses.isEmpty()) return 0

        val sql = """
            INSERT INTO statuses (id, status)
            VALUES (?, ?)
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, statuses, statuses.size) { ps, s ->
            ps.setInt(1, s.id)
            ps.setString(2, s.status)
        }.sumOf { it.sum() }
    }
}
