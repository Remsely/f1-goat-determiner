package dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitJdbcRepository
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalTime

@Import(
    GrandPrixDao::class,
    GrandPrixJdbcRepository::class,
    CircuitDao::class,
    CircuitJdbcRepository::class,
)
class GrandPrixDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: GrandPrixDao

    @Autowired
    lateinit var circuitDao: CircuitDao

    @Autowired
    lateinit var grandPrixJpaRepository: GrandPrixJpaRepository

    @Autowired
    lateinit var circuitJpaRepository: dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitJpaRepository

    @BeforeEach
    fun cleanUp() {
        grandPrixJpaRepository.deleteAllInBatch()
        circuitJpaRepository.deleteAllInBatch()
    }

    private fun insertCircuit(): Circuit {
        val circuit = Circuit(id = 1, ref = "monza", name = "Monza", locality = "Monza", country = "Italy")
        circuitDao.upsertAll(listOf(circuit))
        return circuit
    }

    @Test
    fun `upsertAll inserts grand prix`() {
        insertCircuit()
        val races = listOf(
            GrandPrix(
                id = 1,
                season = 2024,
                round = 1,
                circuitId = 1,
                name = "Bahrain GP",
                date = LocalDate.of(2024, 3, 2),
                time = LocalTime.of(15, 0),
            ),
            GrandPrix(
                id = 2,
                season = 2024,
                round = 2,
                circuitId = 1,
                name = "Saudi Arabian GP",
                date = LocalDate.of(2024, 3, 9),
                time = null,
            ),
        )
        dao.upsertAll(races) shouldBe 2
        dao.count() shouldBe 2
    }

    @Test
    fun `findBySeasonAndRound returns correct grand prix`() {
        insertCircuit()
        dao.upsertAll(
            listOf(
                GrandPrix(
                    id = 1,
                    season = 2024,
                    round = 1,
                    circuitId = 1,
                    name = "Bahrain GP",
                    date = LocalDate.of(2024, 3, 2),
                    time = null,
                ),
                GrandPrix(
                    id = 2,
                    season = 2024,
                    round = 2,
                    circuitId = 1,
                    name = "Saudi Arabian GP",
                    date = LocalDate.of(2024, 3, 9),
                    time = null,
                ),
            ),
        )

        val found = dao.findBySeasonAndRound(2024, 2)

        found.shouldNotBeNull()
        found.name shouldBe "Saudi Arabian GP"
    }

    @Test
    fun `findBySeasonAndRound returns null when not found`() {
        dao.findBySeasonAndRound(2024, 99).shouldBeNull()
    }

    @Test
    fun `findMaxRoundBySeason returns max round`() {
        insertCircuit()
        dao.upsertAll(
            listOf(
                GrandPrix(
                    id = 1,
                    season = 2024,
                    round = 1,
                    circuitId = 1,
                    name = "R1",
                    date = LocalDate.of(2024, 3, 2),
                    time = null,
                ),
                GrandPrix(
                    id = 2,
                    season = 2024,
                    round = 5,
                    circuitId = 1,
                    name = "R5",
                    date = LocalDate.of(2024, 5, 2),
                    time = null,
                ),
                GrandPrix(
                    id = 3,
                    season = 2024,
                    round = 3,
                    circuitId = 1,
                    name = "R3",
                    date = LocalDate.of(2024, 4, 2),
                    time = null,
                ),
            ),
        )
        dao.findMaxRoundBySeason(2024) shouldBe 5
    }

    @Test
    fun `findMaxRoundBySeason returns null for non-existing season`() {
        dao.findMaxRoundBySeason(1900).shouldBeNull()
    }

    @Test
    fun `findAllSeasons returns distinct sorted seasons`() {
        insertCircuit()
        dao.upsertAll(
            listOf(
                GrandPrix(
                    id = 1,
                    season = 2023,
                    round = 1,
                    circuitId = 1,
                    name = "R1",
                    date = LocalDate.of(2023, 3, 2),
                    time = null,
                ),
                GrandPrix(
                    id = 2,
                    season = 2024,
                    round = 1,
                    circuitId = 1,
                    name = "R1",
                    date = LocalDate.of(2024, 3, 2),
                    time = null,
                ),
                GrandPrix(
                    id = 3,
                    season = 2023,
                    round = 2,
                    circuitId = 1,
                    name = "R2",
                    date = LocalDate.of(2023, 4, 2),
                    time = null,
                ),
            ),
        )
        dao.findAllSeasons() shouldContainExactly listOf(2023, 2024)
    }
}
