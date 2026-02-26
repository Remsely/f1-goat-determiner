package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.AverageSpeedDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ConstructorDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.FastestLapDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.FastestLapTimeDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultDto
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.ResultTimeDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RaceResultMappingExtensionsTest {

    private val driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton")
    private val constructor = ConstructorDto(constructorId = "mercedes", name = "Mercedes")

    @Test
    fun `toDomain maps all fields correctly for classified result`() {
        val dto = ResultDto(
            number = "44",
            position = "1",
            positionText = "1",
            points = "26",
            driver = driver,
            constructor = constructor,
            grid = "1",
            laps = "57",
            status = "Finished",
            time = ResultTimeDto(millis = "5504742", time = "1:31:44.742"),
            fastestLap = FastestLapDto(
                rank = "1",
                lap = "39",
                time = FastestLapTimeDto(time = "1:32.608"),
                averageSpeed = AverageSpeedDto(units = "kph", speed = "210.383"),
            ),
        )

        val result = dto.toDomain(
            id = 1,
            grandPrixId = 100,
            driverId = 10,
            constructorId = 20,
            statusId = 1,
        )

        result.id shouldBe 1
        result.grandPrixId shouldBe 100
        result.driverId shouldBe 10
        result.constructorId shouldBe 20
        result.number shouldBe 44
        result.grid shouldBe 1
        result.position shouldBe 1
        result.positionText shouldBe "1"
        result.positionOrder shouldBe 1
        result.points shouldBe BigDecimal("26")
        result.laps shouldBe 57
        result.time shouldBe "1:31:44.742"
        result.milliseconds shouldBe 5504742L
        result.fastestLap shouldBe 39
        result.fastestLapRank shouldBe 1
        result.fastestLapTime shouldBe "1:32.608"
        result.fastestLapSpeed shouldBe BigDecimal("210.383")
        result.statusId shouldBe 1
    }

    @Test
    fun `toDomain handles retired driver with null position and no fastest lap`() {
        val dto = ResultDto(
            number = "44",
            position = null,
            positionText = "R",
            points = "0",
            driver = driver,
            constructor = constructor,
            grid = "5",
            laps = "20",
            status = "Engine",
            time = null,
            fastestLap = null,
        )

        val result = dto.toDomain(
            id = 2,
            grandPrixId = 100,
            driverId = 10,
            constructorId = 20,
            statusId = 5,
        )

        result.position.shouldBeNull()
        result.positionText shouldBe "R"
        result.positionOrder shouldBe Int.MAX_VALUE
        result.time.shouldBeNull()
        result.milliseconds.shouldBeNull()
        result.fastestLap.shouldBeNull()
        result.fastestLapRank.shouldBeNull()
        result.fastestLapTime.shouldBeNull()
        result.fastestLapSpeed.shouldBeNull()
    }
}
