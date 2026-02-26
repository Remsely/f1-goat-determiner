package dev.remsely.f1goatdeterminer.datasync.db.repository.constructor

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ConstructorJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(constructors: List<Constructor>): Int {
        if (constructors.isEmpty()) return 0

        val sql = """
            INSERT INTO constructors (id, ref, name, nationality)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                ref = EXCLUDED.ref,
                name = EXCLUDED.name,
                nationality = EXCLUDED.nationality
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, constructors, constructors.size) { ps, c ->
            ps.setInt(1, c.id)
            ps.setString(2, c.ref)
            ps.setString(3, c.name)
            ps.setString(4, c.nationality)
        }.sumOf { it.sum() }
    }
}
