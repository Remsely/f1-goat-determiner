package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.CircuitDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.LocationDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CircuitMappingExtensionsTest {
    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = CircuitDto(
            circuitId = "monza",
            circuitName = "Autodromo Nazionale Monza",
            location = LocationDto(locality = "Monza", country = "Italy"),
        )

        val result = dto.toDomain()

        result.id.shouldBeNull()
        result.ref shouldBe "monza"
        result.name shouldBe "Autodromo Nazionale Monza"
        result.locality shouldBe "Monza"
        result.country shouldBe "Italy"
    }

    @Test
    fun `toDomain handles null location`() {
        val dto = CircuitDto(
            circuitId = "unknown",
            circuitName = "Unknown Circuit",
            location = null,
        )

        val result = dto.toDomain()

        result.locality.shouldBeNull()
        result.country.shouldBeNull()
    }
}
