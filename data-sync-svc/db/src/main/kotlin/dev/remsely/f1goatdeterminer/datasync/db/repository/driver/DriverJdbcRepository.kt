package dev.remsely.f1goatdeterminer.datasync.db.repository.driver

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Types

@Component
class DriverJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(drivers: List<Driver>): Int {
        if (drivers.isEmpty()) return 0

        val sql = """
            INSERT INTO drivers (id, ref, number, code, forename, surname, dob, nationality)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                ref = EXCLUDED.ref,
                number = EXCLUDED.number,
                code = EXCLUDED.code,
                forename = EXCLUDED.forename,
                surname = EXCLUDED.surname,
                dob = EXCLUDED.dob,
                nationality = EXCLUDED.nationality
        """.trimIndent()

        return jdbcTemplate.batchUpdate(sql, drivers, drivers.size) { ps, d ->
            val number = d.number
            val dob = d.dateOfBirth

            ps.setInt(1, d.id)
            ps.setString(2, d.ref)

            if (number != null) ps.setInt(3, number) else ps.setNull(3, Types.INTEGER)

            ps.setString(4, d.code)
            ps.setString(5, d.forename)
            ps.setString(6, d.surname)

            if (dob != null) ps.setDate(7, Date.valueOf(dob)) else ps.setNull(7, Types.DATE)

            ps.setString(8, d.nationality)
        }.sumOf { it.sum() }
    }
}
