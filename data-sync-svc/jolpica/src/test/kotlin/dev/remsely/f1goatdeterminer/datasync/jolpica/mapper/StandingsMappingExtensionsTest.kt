package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorStandingDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverStandingDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StandingsMappingExtensionsTest {
    @Test
    fun `DriverStandingDto toFetchedDriverStanding maps all fields correctly`() {
        val dto = DriverStandingDto(
            position = "1",
            positionText = "1",
            points = "26",
            wins = "1",
            driver = DriverDto(driverId = "max_verstappen", givenName = "Max", familyName = "Verstappen"),
        )

        val result = dto.toFetchedDriverStanding(season = 2024, round = 1)

        result.season shouldBe 2024
        result.round shouldBe 1
        result.driverRef shouldBe "max_verstappen"
        result.points shouldBe BigDecimal("26")
        result.position shouldBe 1
        result.positionText shouldBe "1"
        result.wins shouldBe 1
    }

    @Test
    fun `ConstructorStandingDto toFetchedConstructorStanding maps all fields correctly`() {
        val dto = ConstructorStandingDto(
            position = "2",
            positionText = "2",
            points = "44.5",
            wins = "3",
            constructor = ConstructorDto(constructorId = "ferrari", name = "Ferrari"),
        )

        val result = dto.toFetchedConstructorStanding(season = 2024, round = 1)

        result.season shouldBe 2024
        result.round shouldBe 1
        result.constructorRef shouldBe "ferrari"
        result.points shouldBe BigDecimal("44.5")
        result.position shouldBe 2
        result.positionText shouldBe "2"
        result.wins shouldBe 3
    }
}
