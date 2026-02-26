package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DriverMappingExtensionsTest {
    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = DriverDto(
            driverId = "hamilton",
            permanentNumber = "44",
            code = "HAM",
            givenName = "Lewis",
            familyName = "Hamilton",
            dateOfBirth = "1985-01-07",
            nationality = "British",
        )

        val result = dto.toDomain(id = 10)

        result.id shouldBe 10
        result.ref shouldBe "hamilton"
        result.number shouldBe 44
        result.code shouldBe "HAM"
        result.forename shouldBe "Lewis"
        result.surname shouldBe "Hamilton"
        result.dateOfBirth shouldBe LocalDate.of(1985, 1, 7)
        result.nationality shouldBe "British"
    }

    @Test
    fun `toDomain handles null optional fields`() {
        val dto = DriverDto(
            driverId = "abate",
            givenName = "Carlo",
            familyName = "Abate",
        )

        val result = dto.toDomain(id = 1)

        result.number.shouldBeNull()
        result.code.shouldBeNull()
        result.dateOfBirth.shouldBeNull()
        result.nationality.shouldBeNull()
    }
}
