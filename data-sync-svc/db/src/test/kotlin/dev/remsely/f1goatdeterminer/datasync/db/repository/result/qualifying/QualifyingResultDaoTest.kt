package dev.remsely.f1goatdeterminer.datasync.db.repository.result.qualifying

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
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(
    QualifyingResultDao::class,
    QualifyingResultJdbcRepository::class,
    GrandPrixDao::class,
    GrandPrixJdbcRepository::class,
    CircuitDao::class,
    CircuitJdbcRepository::class,
    DriverDao::class,
    DriverJdbcRepository::class,
    ConstructorDao::class,
    ConstructorJdbcRepository::class,
    DbTestDataHelper::class,
)
class QualifyingResultDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: QualifyingResultDao

    @Autowired
    lateinit var jpaRepository: QualifyingResultJpaRepository

    @Autowired
    lateinit var grandPrixJpaRepository: GrandPrixJpaRepository

    @Autowired
    lateinit var testData: DbTestDataHelper

    private var raceId: Int = 0
    private var driverId: Int = 0
    private var constructorId: Int = 0

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
        grandPrixJpaRepository.deleteAllInBatch()

        val circuitId = testData.insertMonzaCircuit()
        driverId = testData.insertHamiltonDriver()
        constructorId = testData.insertMercedesConstructor()
        raceId = testData.insertItalianGp2024(circuitId)
    }

    @Test
    fun `upsertAll inserts qualifying results`() {
        val results = listOf(
            QualifyingResult(
                grandPrixId = raceId,
                driverId = driverId,
                constructorId = constructorId,
                number = 44,
                position = 1,
                q1 = "1:23.456",
                q2 = "1:22.345",
                q3 = "1:21.234",
            ),
        )

        dao.upsertAll(results) shouldBe 1
        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll updates existing qualifying results`() {
        dao.upsertAll(
            listOf(
                QualifyingResult(
                    grandPrixId = raceId,
                    driverId = driverId,
                    constructorId = constructorId,
                    number = 44,
                    position = 3,
                    q1 = "1:25.000",
                    q2 = null,
                    q3 = null,
                ),
            ),
        )

        dao.upsertAll(
            listOf(
                QualifyingResult(
                    grandPrixId = raceId,
                    driverId = driverId,
                    constructorId = constructorId,
                    number = 44,
                    position = 1,
                    q1 = "1:23.456",
                    q2 = "1:22.345",
                    q3 = "1:21.234",
                ),
            ),
        )

        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll with empty list returns 0`() {
        dao.upsertAll(emptyList()) shouldBe 0
    }

    @Test
    fun `count returns 0 for empty table`() {
        dao.count() shouldBe 0
    }
}
