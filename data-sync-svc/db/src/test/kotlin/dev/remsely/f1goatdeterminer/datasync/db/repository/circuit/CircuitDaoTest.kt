package dev.remsely.f1goatdeterminer.datasync.db.repository.circuit

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(CircuitDao::class, CircuitJdbcRepository::class)
class CircuitDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: CircuitDao

    @Autowired
    lateinit var jpaRepository: CircuitJpaRepository

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    @Test
    fun `upsertAll inserts new circuits`() {
        val circuits = listOf(
            Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"),
            Circuit(ref = "spa", name = "Spa-Francorchamps", locality = "Spa", country = "Belgium"),
        )

        val result = dao.upsertAll(circuits)

        result shouldBe 2
        dao.count() shouldBe 2
    }

    @Test
    fun `upsertAll updates existing circuits`() {
        dao.upsertAll(
            listOf(
                Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"),
            ),
        )

        dao.upsertAll(
            listOf(
                Circuit(ref = "monza", name = "Autodromo Nazionale Monza", locality = "Monza", country = "Italy"),
            ),
        )

        dao.count() shouldBe 1

        val found = dao.findByRef("monza")

        found.shouldNotBeNull()
        found.name shouldBe "Autodromo Nazionale Monza"
    }

    @Test
    fun `upsertAll with empty list returns 0`() {
        dao.upsertAll(emptyList()) shouldBe 0
    }

    @Test
    fun `findById returns null for non-existing circuit`() {
        dao.findById(999).shouldBeNull()
    }

    @Test
    fun `count returns 0 for empty table`() {
        dao.count() shouldBe 0
    }
}
