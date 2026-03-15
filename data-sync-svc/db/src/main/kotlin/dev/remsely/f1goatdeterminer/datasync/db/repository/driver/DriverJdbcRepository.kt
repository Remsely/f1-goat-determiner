package dev.remsely.f1goatdeterminer.datasync.db.repository.driver

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Types

@Component
class DriverJdbcRepository(private val jdbcTemplate: JdbcTemplate) {
    fun upsertAll(drivers: List<Driver>): Int {
        if (drivers.isEmpty()) return 0

        val sql = """
            INSERT INTO drivers (ref, number, code, forename, surname, dob, nationality)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (ref) DO UPDATE SET
                number = EXCLUDED.number,
                code = EXCLUDED.code,
                forename = EXCLUDED.forename,
                surname = EXCLUDED.surname,
                dob = EXCLUDED.dob,
                nationality = EXCLUDED.nationality
        """.trimIndent()

        val countBefore = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM drivers")!!

        jdbcTemplate.batchUpdate(sql, drivers, drivers.size) { ps, d ->
            val number = d.number
            val dob = d.dateOfBirth

            ps.setString(1, d.ref)

            if (number != null) ps.setInt(2, number) else ps.setNull(2, Types.INTEGER)

            ps.setString(3, d.code)
            ps.setString(4, d.forename)
            ps.setString(5, d.surname)

            if (dob != null) ps.setDate(6, Date.valueOf(dob)) else ps.setNull(6, Types.DATE)

            ps.setString(7, d.nationality)
        }

        val countAfter = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM drivers")!!
        return (countAfter - countBefore).toInt()
    }
}
