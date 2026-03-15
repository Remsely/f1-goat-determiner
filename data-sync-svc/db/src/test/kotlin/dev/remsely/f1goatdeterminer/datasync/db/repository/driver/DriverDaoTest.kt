package dev.remsely.f1goatdeterminer.datasync.db.repository.driver

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestDrivers
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@Import(DriverDao::class, DriverJdbcRepository::class)
class DriverDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: DriverDao

    @Autowired
    lateinit var jpaRepository: DriverJpaRepository

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    @Test
    fun `upsertAll inserts new drivers`() {
        val drivers = listOf(
            TestDrivers.hamilton,
            TestDrivers.verstappen,
        )

        val result = dao.upsertAll(drivers)

        result shouldBe 2
        dao.count() shouldBe 2
    }

    @Test
    fun `upsertAll updates existing drivers`() {
        dao.upsertAll(listOf(TestDrivers.hamilton))
        dao.upsertAll(
            listOf(
                Driver(
                    ref = "hamilton",
                    number = 44,
                    code = "HAM",
                    forename = "Lewis",
                    surname = "Hamilton-Updated",
                    dateOfBirth = LocalDate.of(1985, 1, 7),
                    nationality = "British",
                ),
            ),
        )

        dao.count() shouldBe 1

        val found = dao.findByRef("hamilton")

        found.shouldNotBeNull()
        found.surname shouldBe "Hamilton-Updated"
    }

    @Test
    fun `upsertAll with empty list returns 0`() {
        dao.upsertAll(emptyList()) shouldBe 0
    }

    @Test
    fun `upsertAll handles nullable fields`() {
        val driver = Driver(
            ref = "farina",
            number = null,
            code = null,
            forename = "Nino",
            surname = "Farina",
            dateOfBirth = null,
            nationality = null,
        )

        dao.upsertAll(listOf(driver)) shouldBe 1

        val found = dao.findByRef("farina")

        found.shouldNotBeNull()
        found.number.shouldBeNull()
        found.code.shouldBeNull()
        found.dateOfBirth.shouldBeNull()
    }

    @Test
    fun `findById returns null for non-existing driver`() {
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
                TestDrivers.hamilton,
                TestDrivers.verstappen,
            ),
        )

        val mapping = dao.findAllRefToId()

        mapping.size shouldBe 2
        mapping.containsKey("hamilton") shouldBe true
        mapping.containsKey("verstappen") shouldBe true
    }

    @Test
    fun `count returns 0 for empty table`() {
        dao.count() shouldBe 0
    }
}
