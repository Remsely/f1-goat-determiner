package dev.remsely.f1goatdeterminer.datasync.db.repository.constructor

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(ConstructorDao::class, ConstructorJdbcRepository::class)
class ConstructorDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: ConstructorDao

    @Autowired
    lateinit var jpaRepository: ConstructorJpaRepository

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    @Test
    fun `upsertAll inserts new constructors`() {
        val constructors = listOf(
            Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"),
            Constructor(ref = "mclaren", name = "McLaren", nationality = "British"),
        )

        val result = dao.upsertAll(constructors)

        result shouldBe 2
        dao.count() shouldBe 2
    }

    @Test
    fun `upsertAll updates existing constructors`() {
        dao.upsertAll(listOf(Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian")))
        dao.upsertAll(listOf(Constructor(ref = "ferrari", name = "Scuderia Ferrari", nationality = "Italian")))

        dao.count() shouldBe 1

        val found = dao.findByRef("ferrari")

        found.shouldNotBeNull()
        found.name shouldBe "Scuderia Ferrari"
    }

    @Test
    fun `upsertAll with empty list returns 0`() {
        dao.upsertAll(emptyList()) shouldBe 0
    }

    @Test
    fun `findById returns null for non-existing constructor`() {
        dao.findById(999).shouldBeNull()
    }

    @Test
    fun `findByRef returns null for non-existing ref`() {
        dao.findByRef("nonexistent").shouldBeNull()
    }

    @Test
    fun `findAllRefToId returns correct mapping`() {
        dao.upsertAll(
            listOf(
                Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"),
                Constructor(ref = "mclaren", name = "McLaren", nationality = "British"),
            ),
        )

        val mapping = dao.findAllRefToId()

        mapping.size shouldBe 2
        mapping.containsKey("ferrari") shouldBe true
        mapping.containsKey("mclaren") shouldBe true
    }

    @Test
    fun `count returns 0 for empty table`() {
        dao.count() shouldBe 0
    }
}
