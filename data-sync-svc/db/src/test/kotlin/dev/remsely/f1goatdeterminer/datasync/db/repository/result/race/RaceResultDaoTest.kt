package dev.remsely.f1goatdeterminer.datasync.db.repository.result.race

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.db.fixture.DbTestDataHelper
import dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.db.repository.constructor.ConstructorDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.constructor.ConstructorJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.db.repository.driver.DriverDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.driver.DriverJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix.GrandPrixDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix.GrandPrixJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix.GrandPrixJpaRepository
import dev.remsely.f1goatdeterminer.datasync.db.repository.result.status.StatusDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.result.status.StatusJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResult
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@Import(
    RaceResultDao::class,
    RaceResultJdbcRepository::class,
    GrandPrixDao::class,
    GrandPrixJdbcRepository::class,
    CircuitDao::class,
    CircuitJdbcRepository::class,
    DriverDao::class,
    DriverJdbcRepository::class,
    ConstructorDao::class,
    ConstructorJdbcRepository::class,
    StatusDao::class,
    StatusJdbcRepository::class,
    DbTestDataHelper::class,
)
class RaceResultDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: RaceResultDao

    @Autowired
    lateinit var jpaRepository: RaceResultJpaRepository

    @Autowired
    lateinit var grandPrixJpaRepository: GrandPrixJpaRepository

    @Autowired
    lateinit var testData: DbTestDataHelper

    @Autowired
    lateinit var statusDao: StatusDao

    private var raceId: Int = 0
    private var driverId: Int = 0
    private var constructorId: Int = 0
    private var statusId: Int = 0

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
        grandPrixJpaRepository.deleteAllInBatch()

        val circuitId = testData.insertMonzaCircuit()
        driverId = testData.insertHamiltonDriver()
        constructorId = testData.insertMercedesConstructor()
        statusDao.upsertAll(listOf(Status(id = 1, status = "Finished")))
        statusId = 1
        raceId = testData.insertItalianGp2024(circuitId)
    }

    @Test
    fun `upsertAll inserts race results`() {
        val results = listOf(testRaceResult())

        dao.upsertAll(results) shouldBe 1
        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll updates existing race results`() {
        dao.upsertAll(listOf(testRaceResult()))
        dao.upsertAll(listOf(testRaceResult().copy(points = BigDecimal("25.0"))))

        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll with empty list returns 0`() {
        dao.upsertAll(emptyList()) shouldBe 0
    }

    @Test
    fun `upsertAll handles nullable fields`() {
        val result = RaceResult(
            grandPrixId = raceId,
            driverId = driverId,
            constructorId = constructorId,
            number = null,
            grid = 1,
            position = null,
            positionText = "R",
            positionOrder = 1,
            points = BigDecimal("0.0"),
            laps = 30,
            time = null,
            milliseconds = null,
            fastestLap = null,
            fastestLapRank = null,
            fastestLapTime = null,
            fastestLapSpeed = null,
            statusId = statusId,
        )

        dao.upsertAll(listOf(result)) shouldBe 1
        dao.count() shouldBe 1
    }

    @Test
    fun `count returns 0 for empty table`() {
        dao.count() shouldBe 0
    }

    private fun testRaceResult() = RaceResult(
        grandPrixId = raceId,
        driverId = driverId,
        constructorId = constructorId,
        number = 44,
        grid = 1,
        position = 1,
        positionText = "1",
        positionOrder = 1,
        points = BigDecimal("10.0"),
        laps = 53,
        time = "1:30:00.000",
        milliseconds = 5400000L,
        fastestLap = 45,
        fastestLapRank = 1,
        fastestLapTime = "1:21.234",
        fastestLapSpeed = BigDecimal("230.5"),
        statusId = statusId,
    )
}
