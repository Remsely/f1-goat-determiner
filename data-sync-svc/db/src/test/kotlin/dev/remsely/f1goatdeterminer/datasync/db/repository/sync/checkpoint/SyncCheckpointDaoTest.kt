package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job.SyncJobDao
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
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

@Import(SyncCheckpointDao::class, SyncJobDao::class)
class SyncCheckpointDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: SyncCheckpointDao

    @Autowired
    lateinit var jobDao: SyncJobDao

    @Autowired
    lateinit var checkpointJpaRepository: SyncCheckpointJpaRepository

    @Autowired
    lateinit var jobJpaRepository: dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job.SyncJobJpaRepository

    private val now: LocalDateTime = LocalDateTime.of(2024, 6, 1, 12, 0)
    private var jobId: Long = 0

    @BeforeEach
    fun cleanUp() {
        checkpointJpaRepository.deleteAllInBatch()
        jobJpaRepository.deleteAllInBatch()
        val job = jobDao.save(
            SyncJob(
                id = null,
                type = SyncJob.Type.FULL,
                status = SyncStatus.IN_PROGRESS,
                startedAt = now,
                updatedAt = now,
                completedAt = null,
                errorMessage = null,
                totalRequests = 0,
                failedRequests = 0,
            ),
        )
        jobId = job.id!!
    }

    private fun createCheckpoint(
        entityType: SyncEntityType = SyncEntityType.CIRCUITS,
        status: SyncStatus = SyncStatus.PENDING,
    ): SyncCheckpoint = SyncCheckpoint.initPending(jobId, entityType).copy(status = status)

    @Test
    fun `save persists and returns checkpoint with generated id`() {
        val saved = dao.save(createCheckpoint())

        saved.id.shouldNotBeNull()
        saved.jobId shouldBe jobId
        saved.entityType shouldBe SyncEntityType.CIRCUITS
    }

    @Test
    fun `saveAll persists multiple checkpoints`() {
        val checkpoints = SyncEntityType.syncOrdered.map { createCheckpoint(entityType = it) }

        val saved = dao.saveAll(checkpoints)

        saved shouldHaveSize SyncEntityType.syncOrdered.size
    }

    @Test
    fun `findById returns saved checkpoint`() {
        val saved = dao.save(createCheckpoint())

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.entityType shouldBe SyncEntityType.CIRCUITS
    }

    @Test
    fun `findById returns null for non-existing checkpoint`() {
        dao.findById(999L).shouldBeNull()
    }

    @Test
    fun `findByJobId returns all checkpoints for job`() {
        dao.saveAll(
            listOf(
                createCheckpoint(entityType = SyncEntityType.CIRCUITS),
                createCheckpoint(entityType = SyncEntityType.DRIVERS),
            ),
        )

        dao.findByJobId(jobId) shouldHaveSize 2
    }

    @Test
    fun `findByJobIdAndEntityType returns correct checkpoint`() {
        dao.save(createCheckpoint(entityType = SyncEntityType.CIRCUITS))
        dao.save(createCheckpoint(entityType = SyncEntityType.DRIVERS))

        val found = dao.findByJobIdAndEntityType(jobId, SyncEntityType.DRIVERS)

        found.shouldNotBeNull()
        found.entityType shouldBe SyncEntityType.DRIVERS
    }

    @Test
    fun `findByJobIdAndEntityType returns null when not found`() {
        dao.findByJobIdAndEntityType(jobId, SyncEntityType.STATUSES).shouldBeNull()
    }

    @Test
    fun `findPendingByJobId returns only pending checkpoints`() {
        dao.save(createCheckpoint(entityType = SyncEntityType.CIRCUITS, status = SyncStatus.PENDING))
        dao.save(createCheckpoint(entityType = SyncEntityType.DRIVERS, status = SyncStatus.COMPLETED))

        dao.findPendingByJobId(jobId) shouldHaveSize 1
    }

    @Test
    fun `updateStatus changes checkpoint status`() {
        val saved = dao.save(createCheckpoint())

        dao.updateStatus(saved.id!!, SyncStatus.IN_PROGRESS)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.IN_PROGRESS
    }

    @Test
    fun `updateStatus with error message`() {
        val saved = dao.save(createCheckpoint())

        dao.updateStatus(saved.id!!, SyncStatus.FAILED, "Connection timeout")

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.FAILED
        found.errorMessage shouldBe "Connection timeout"
    }

    @Test
    fun `updateProgress updates offset and counters`() {
        val saved = dao.save(createCheckpoint())

        dao.updateProgress(saved.id!!, lastOffset = 100, lastSeason = 2024, lastRound = 5, recordsSynced = 500)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.lastOffset shouldBe 100
        found.lastSeason shouldBe 2024
        found.lastRound shouldBe 5
        found.recordsSynced shouldBe 500
    }

    @Test
    fun `incrementRetryCount increments counter`() {
        val saved = dao.save(createCheckpoint())

        dao.incrementRetryCount(saved.id!!)
        dao.incrementRetryCount(saved.id!!)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.retryCount shouldBe 2
    }

    @Test
    fun `complete marks checkpoint as completed`() {
        val saved = dao.save(createCheckpoint())
        val completedAt = now.plusHours(1)

        dao.complete(saved.id!!, recordsSynced = 1000, completedAt = completedAt)

        val found = dao.findById(saved.id!!)

        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.COMPLETED
        found.recordsSynced shouldBe 1000
        found.completedAt shouldBe completedAt
    }

    @Test
    fun `fail marks checkpoint as failed with error`() {
        val saved = dao.save(createCheckpoint())

        dao.fail(saved.id!!, "API rate limit exceeded")

        val found = dao.findById(saved.id!!)
        found.shouldNotBeNull()
        found.status shouldBe SyncStatus.FAILED
        found.errorMessage shouldBe "API rate limit exceeded"
    }
}
