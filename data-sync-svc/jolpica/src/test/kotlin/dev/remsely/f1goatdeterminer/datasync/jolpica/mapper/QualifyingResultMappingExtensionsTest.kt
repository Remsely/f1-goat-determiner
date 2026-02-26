package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.QualifyingResultDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QualifyingResultMappingExtensionsTest {

    private val driver = DriverDto(driverId = "max_verstappen", givenName = "Max", familyName = "Verstappen")
    private val constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull")

    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = QualifyingResultDto(
            number = "1",
            position = "1",
            driver = driver,
            constructor = constructor,
            q1 = "1:30.031",
            q2 = "1:29.374",
            q3 = "1:29.179",
        )

        val result = dto.toDomain(id = 1, grandPrixId = 100, driverId = 10, constructorId = 20)

        result.id shouldBe 1
        result.grandPrixId shouldBe 100
        result.driverId shouldBe 10
        result.constructorId shouldBe 20
        result.number shouldBe 1
        result.position shouldBe 1
        result.q1 shouldBe "1:30.031"
        result.q2 shouldBe "1:29.374"
        result.q3 shouldBe "1:29.179"
    }

    @Test
    fun `toDomain handles eliminated in Q1 with null Q2 and Q3`() {
        val dto = QualifyingResultDto(
            number = "2",
            position = "20",
            driver = driver,
            constructor = constructor,
            q1 = "1:32.500",
            q2 = null,
            q3 = null,
        )

        val result = dto.toDomain(id = 2, grandPrixId = 100, driverId = 10, constructorId = 20)

        result.position shouldBe 20
        result.q1 shouldBe "1:32.500"
        result.q2.shouldBeNull()
        result.q3.shouldBeNull()
    }
}
