package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@Import(SyncJobDao::class)
class SyncJobDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: SyncJobDao

    @Autowired
    lateinit var jpaRepository: SyncJobJpaRepository

    private val now: LocalDateTime = LocalDateTime.of(2024, 6, 1, 12, 0)

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    private fun createJob(
        type: SyncJob.Type = SyncJob.Type.FULL,
        status: SyncStatus = SyncStatus.PENDING,
        startedAt: LocalDateTime = now,
    ): SyncJob = SyncJob(
        id = null,
        type = type,
        status = status,
        startedAt = startedAt,
        updatedAt = startedAt,
        completedAt = null,
        errorMessage = null,
        totalRequests = 0,
        failedRequests = 0,
    )

    @Test
    fun `save persists and returns job with generated id`() {
        val saved = dao.save(createJob())

        saved.id.shouldNotBeNull()
        saved.type shouldBe SyncJob.Type.FULL
        saved.status shouldBe SyncStatus.PENDING
    }

    @Test
    fun `findById returns saved job`() {
        val saved = dao.save(createJob())

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.id shouldBe saved.id
    }

    @Test
    fun `findById returns null for non-existing job`() {
        dao.findById(999L).shouldBeNull()
    }

    @Test
    fun `findLatest returns most recently started job`() {
        dao.save(createJob(startedAt = now.minusHours(2)))
        dao.save(createJob(startedAt = now))

        val latest = dao.findLatest()

        latest.shouldNotBeNull()
        latest.startedAt shouldBe now
    }

    @Test
    fun `findByStatus returns jobs with matching status`() {
        dao.save(createJob(status = SyncStatus.PENDING))
        dao.save(createJob(status = SyncStatus.COMPLETED))
        dao.save(createJob(status = SyncStatus.PENDING))

        dao.findByStatus(SyncStatus.PENDING) shouldHaveSize 2
        dao.findByStatus(SyncStatus.COMPLETED) shouldHaveSize 1
        dao.findByStatus(SyncStatus.FAILED) shouldHaveSize 0
    }

    @Test
    fun `findByStatusIn returns jobs with any of given statuses`() {
        dao.save(createJob(status = SyncStatus.PENDING))
        dao.save(createJob(status = SyncStatus.FAILED))
        dao.save(createJob(status = SyncStatus.COMPLETED))

        dao.findByStatusIn(listOf(SyncStatus.PENDING, SyncStatus.FAILED)) shouldHaveSize 2
    }

    @Test
    fun `findActiveByType returns active job`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.IN_PROGRESS))

        dao.findActiveByType(SyncJob.Type.FULL).shouldNotBeNull()
        dao.findActiveByType(SyncJob.Type.INCREMENTAL).shouldBeNull()
    }

    @Test
    fun `findResumable returns first resumable job`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.FAILED))
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.COMPLETED))

        dao.findResumable(SyncJob.Type.FULL).shouldNotBeNull()
    }

    @Test
    fun `updateStatus changes job status`() {
        val saved = dao.save(createJob(status = SyncStatus.PENDING))

        dao.updateStatus(saved.id!!, SyncStatus.IN_PROGRESS)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.IN_PROGRESS
    }

    @Test
    fun `updateProgress changes request counters`() {
        val saved = dao.save(createJob())

        dao.updateProgress(saved.id!!, totalRequests = 100, failedRequests = 5)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.totalRequests shouldBe 100
        found.failedRequests shouldBe 5
    }

    @Test
    fun `complete sets status and completedAt`() {
        val saved = dao.save(createJob(status = SyncStatus.IN_PROGRESS))
        val completedAt = now.plusHours(1)

        dao.complete(saved.id!!, SyncStatus.COMPLETED, completedAt)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.COMPLETED
        found.completedAt shouldBe completedAt
    }
}
