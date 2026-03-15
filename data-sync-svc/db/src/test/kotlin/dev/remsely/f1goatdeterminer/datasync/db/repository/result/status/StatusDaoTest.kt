package dev.remsely.f1goatdeterminer.datasync.db.repository.result.status

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(StatusDao::class, StatusJdbcRepository::class)
class StatusDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: StatusDao

    @Autowired
    lateinit var jpaRepository: StatusJpaRepository

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    @Test
    fun `upsertAll inserts new statuses`() {
        val statuses = listOf(
            Status(id = 1, status = "Finished"),
            Status(id = 2, status = "Disqualified"),
            Status(id = 3, status = "Accident"),
        )

        val result = dao.upsertAll(statuses)

        result shouldBe 3
        dao.count() shouldBe 3
    }

    @Test
    fun `upsertAll updates existing statuses`() {
        dao.upsertAll(listOf(Status(id = 1, status = "Finished")))
        dao.upsertAll(listOf(Status(id = 1, status = "Finished (updated)")))

        dao.count() shouldBe 1

        val found = dao.findById(1)

        found.shouldNotBeNull()
        found.status shouldBe "Finished (updated)"
    }

    @Test
    fun `findAll returns all statuses`() {
        dao.upsertAll(
            listOf(
                Status(id = 1, status = "Finished"),
                Status(id = 2, status = "Retired"),
            ),
        )
        dao.findAll() shouldHaveSize 2
    }

    @Test
    fun `findById returns null for non-existing status`() {
        dao.findById(999).shouldBeNull()
    }
}
