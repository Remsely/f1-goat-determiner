package dev.remsely.f1goatdeterminer.datasync.db.repository.standings.constructor

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
import dev.remsely.f1goatdeterminer.datasync.domain.standings.constructor.ConstructorStanding
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@Import(
    ConstructorStandingDao::class,
    ConstructorStandingJdbcRepository::class,
    GrandPrixDao::class,
    GrandPrixJdbcRepository::class,
    CircuitDao::class,
    CircuitJdbcRepository::class,
    ConstructorDao::class,
    ConstructorJdbcRepository::class,
    DriverDao::class,
    DriverJdbcRepository::class,
    DbTestDataHelper::class,
)
class ConstructorStandingDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: ConstructorStandingDao

    @Autowired
    lateinit var jpaRepository: ConstructorStandingJpaRepository

    @Autowired
    lateinit var grandPrixJpaRepository: GrandPrixJpaRepository

    @Autowired
    lateinit var testData: DbTestDataHelper

    private var raceId: Int = 0
    private var constructorId: Int = 0

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
        grandPrixJpaRepository.deleteAllInBatch()

        val circuitId = testData.insertMonzaCircuit()
        constructorId = testData.insertFerrariConstructor()
        raceId = testData.insertItalianGp2024(circuitId)
    }

    @Test
    fun `upsertAll inserts constructor standings`() {
        val standings = listOf(
            ConstructorStanding(
                grandPrixId = raceId,
                constructorId = constructorId,
                points = BigDecimal("25.0"),
                position = 1,
                positionText = "1",
                wins = 1,
            ),
        )

        dao.upsertAll(standings) shouldBe 1
        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll updates existing standings`() {
        dao.upsertAll(
            listOf(
                ConstructorStanding(
                    grandPrixId = raceId,
                    constructorId = constructorId,
                    points = BigDecimal("25.0"),
                    position = 1,
                    positionText = "1",
                    wins = 1,
                ),
            ),
        )

        dao.upsertAll(
            listOf(
                ConstructorStanding(
                    grandPrixId = raceId,
                    constructorId = constructorId,
                    points = BigDecimal("50.0"),
                    position = 1,
                    positionText = "1",
                    wins = 2,
                ),
            ),
        )

        dao.count() shouldBe 1
    }

    @Test
    fun `upsertAll handles nullable position`() {
        val standing = ConstructorStanding(
            grandPrixId = raceId,
            constructorId = constructorId,
            points = BigDecimal("0.0"),
            position = null,
            positionText = "DSQ",
            wins = 0,
        )

        dao.upsertAll(listOf(standing)) shouldBe 1
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
