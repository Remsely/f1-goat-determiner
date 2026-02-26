package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConstructorMappingExtensionsTest {
    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = ConstructorDto(
            constructorId = "ferrari",
            name = "Ferrari",
            nationality = "Italian",
        )

        val result = dto.toDomain(id = 5)

        result.id shouldBe 5
        result.ref shouldBe "ferrari"
        result.name shouldBe "Ferrari"
        result.nationality shouldBe "Italian"
    }
}
