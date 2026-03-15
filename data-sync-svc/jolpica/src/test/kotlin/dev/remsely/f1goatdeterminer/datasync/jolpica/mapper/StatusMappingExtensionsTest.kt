package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.StatusDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatusMappingExtensionsTest {
    @Test
    fun `toDomain maps statusId and status correctly`() {
        val dto = StatusDto(statusId = "1", count = "8004", status = "Finished")

        val result = dto.toDomain()

        result.id shouldBe 1
        result.status shouldBe "Finished"
    }

    @Test
    fun `toDomain list maps all statuses`() {
        val dtos = listOf(
            StatusDto(statusId = "1", status = "Finished"),
            StatusDto(statusId = "2", status = "Disqualified"),
        )

        val result = dtos.toDomain()

        result.size shouldBe 2
        result[0].id shouldBe 1
        result[1].status shouldBe "Disqualified"
    }
}
