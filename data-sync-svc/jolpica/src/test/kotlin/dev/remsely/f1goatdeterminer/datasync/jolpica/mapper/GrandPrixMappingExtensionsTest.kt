package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.RaceDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class GrandPrixMappingExtensionsTest {
    @Test
    fun `toGrandPrix maps all fields correctly`() {
        val dto = RaceDto(
            season = "2024",
            round = "1",
            raceName = "Bahrain Grand Prix",
            circuit = CircuitDto(circuitId = "bahrain", circuitName = "Bahrain International Circuit"),
            date = "2024-03-02",
            time = "15:00:00Z",
        )

        val result = dto.toGrandPrix(id = 100, circuitId = 42)

        result.id shouldBe 100
        result.season shouldBe 2024
        result.round shouldBe 1
        result.circuitId shouldBe 42
        result.name shouldBe "Bahrain Grand Prix"
        result.date shouldBe LocalDate.of(2024, 3, 2)
        result.time shouldBe LocalTime.of(15, 0)
    }

    @Test
    fun `toGrandPrix handles null time`() {
        val dto = RaceDto(
            season = "1950",
            round = "1",
            raceName = "British Grand Prix",
            circuit = CircuitDto(circuitId = "silverstone", circuitName = "Silverstone"),
            date = "1950-05-13",
            time = null,
        )

        val result = dto.toGrandPrix(id = 1, circuitId = 1)

        result.time.shouldBeNull()
    }
}
