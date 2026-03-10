package dev.remsely.f1goatdeterminer.datasync.db.repository.sync.job

import dev.remsely.f1goatdeterminer.datasync.db.BaseRepositoryTest
import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestSyncJobs
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import dev.remsely.f1goatdeterminer.datasync.domain.sync.job.SyncJob
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@Import(SyncJobDao::class)
class SyncJobDaoTest : BaseRepositoryTest() {

    @Autowired
    lateinit var dao: SyncJobDao

    @Autowired
    lateinit var jpaRepository: SyncJobJpaRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var entityManager: EntityManager

    private val now: LocalDateTime = LocalDateTime.of(2024, 6, 1, 12, 0)

    @BeforeEach
    fun cleanUp() {
        jpaRepository.deleteAllInBatch()
    }

    private fun createJob(
        type: SyncJob.Type = SyncJob.Type.FULL,
        status: SyncStatus = SyncStatus.PENDING,
        startedAt: LocalDateTime = now,
    ): SyncJob = TestSyncJobs.sample(
        id = null,
        type = type,
        status = status,
        startedAt = startedAt,
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
        dao.save(createJob(status = SyncStatus.COMPLETED, startedAt = now.minusHours(2)))
        dao.save(createJob(status = SyncStatus.COMPLETED, startedAt = now))

        val latest = dao.findLatest()

        latest.shouldNotBeNull()
        latest.startedAt shouldBe now
    }

    @Test
    fun `findByStatus returns jobs with matching status`() {
        dao.save(createJob(status = SyncStatus.COMPLETED))
        dao.save(createJob(status = SyncStatus.FAILED))
        dao.save(createJob(status = SyncStatus.COMPLETED))

        dao.findByStatus(SyncStatus.COMPLETED) shouldHaveSize 2
        dao.findByStatus(SyncStatus.FAILED) shouldHaveSize 1
        dao.findByStatus(SyncStatus.PENDING) shouldHaveSize 0
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
    fun `findActiveByType returns matching active job and ignores non-active jobs of other types`() {
        val activeFullJob = dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.IN_PROGRESS))
        dao.save(createJob(type = SyncJob.Type.INCREMENTAL, status = SyncStatus.COMPLETED))

        val found = dao.findActiveByType(SyncJob.Type.FULL)

        found.shouldNotBeNull()
        found.id shouldBe activeFullJob.id
        found.type shouldBe SyncJob.Type.FULL
        found.status shouldBe SyncStatus.IN_PROGRESS
        dao.findActiveByType(SyncJob.Type.INCREMENTAL).shouldBeNull()
    }

    @Test
    fun `findResumable returns first resumable job`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.FAILED))
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.COMPLETED))

        dao.findResumable(SyncJob.Type.FULL).shouldNotBeNull()
    }

    @Test
    fun `findResumable returns latest failed or paused job for requested type only`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.FAILED, startedAt = now.minusHours(2)))
        dao.save(createJob(type = SyncJob.Type.INCREMENTAL, status = SyncStatus.FAILED, startedAt = now.minusHours(1)))
        val latestPausedFullJob = dao.save(
            createJob(type = SyncJob.Type.FULL, status = SyncStatus.PAUSED, startedAt = now),
        )

        val found = dao.findResumable(SyncJob.Type.FULL)

        found.shouldNotBeNull()
        found.id shouldBe latestPausedFullJob.id
        found.type shouldBe SyncJob.Type.FULL
        found.status shouldBe SyncStatus.PAUSED
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

    @Test
    fun `tryClaimResumableJob claims FAILED job updated before threshold`() {
        val saved = dao.save(createJob(status = SyncStatus.FAILED))
        // Force updated_at to a past value
        jdbcTemplate.update(
            "UPDATE sync_jobs SET updated_at = ? WHERE id = ?",
            now.minusMinutes(15),
            saved.id,
        )

        val claimed = dao.tryClaimResumableJob(now.minusMinutes(10))

        claimed.shouldNotBeNull()
        claimed.status shouldBe SyncStatus.PENDING
    }

    @Test
    fun `tryClaimResumableJob does NOT claim recently failed job (cooldown)`() {
        dao.save(createJob(status = SyncStatus.FAILED))
        // Job was just saved — updated_at is NOW, threshold is 10 minutes ago
        val claimed = dao.tryClaimResumableJob(now.minusMinutes(10))

        claimed.shouldBeNull()
    }

    @Test
    fun `tryClaimResumableJob does NOT claim IN_PROGRESS job`() {
        val saved = dao.save(createJob(status = SyncStatus.IN_PROGRESS))
        jdbcTemplate.update(
            "UPDATE sync_jobs SET updated_at = ? WHERE id = ?",
            now.minusMinutes(15),
            saved.id,
        )

        val claimed = dao.tryClaimResumableJob(now.minusMinutes(10))

        claimed.shouldBeNull()
    }

    @Test
    fun `tryClaimResumableJob returns null when no resumable jobs exist`() {
        dao.save(createJob(status = SyncStatus.COMPLETED))

        val claimed = dao.tryClaimResumableJob(now.plusMinutes(1))

        claimed.shouldBeNull()
    }

    @Test
    fun `tryClaimResumableJob returns null when no jobs exist`() {
        val claimed = dao.tryClaimResumableJob(now.plusMinutes(1))

        claimed.shouldBeNull()
    }

    @Test
    fun `tryClaimResumableJob claims only one job when called twice`() {
        val saved = dao.save(createJob(status = SyncStatus.FAILED))
        jdbcTemplate.update(
            "UPDATE sync_jobs SET updated_at = ? WHERE id = ?",
            now.minusMinutes(15),
            saved.id,
        )

        val first = dao.tryClaimResumableJob(now.minusMinutes(10))
        val second = dao.tryClaimResumableJob(now.minusMinutes(10))

        first.shouldNotBeNull()
        second.shouldBeNull()
    }

    @Test
    fun `tryClaimStaleJob claims IN_PROGRESS job older than threshold`() {
        val saved = dao.save(createJob(status = SyncStatus.IN_PROGRESS))
        // Force updated_at to a stale value via JDBC
        val staleTime = now.minusMinutes(30)
        jdbcTemplate.update(
            "UPDATE sync_jobs SET updated_at = ? WHERE id = ?",
            staleTime,
            saved.id,
        )

        val claimed = dao.tryClaimStaleJob(now.minusMinutes(5))

        claimed.shouldNotBeNull()
        claimed.status shouldBe SyncStatus.PENDING
    }

    @Test
    fun `tryClaimStaleJob does NOT claim recently updated IN_PROGRESS job`() {
        dao.save(createJob(status = SyncStatus.IN_PROGRESS))

        // Threshold is 30 minutes ago — job was just created, so it's fresh
        val claimed = dao.tryClaimStaleJob(now.minusMinutes(30))

        claimed.shouldBeNull()
    }

    @Test
    fun `tryClaimStaleJob returns null when no stale jobs exist`() {
        dao.save(createJob(status = SyncStatus.COMPLETED))

        val claimed = dao.tryClaimStaleJob(now.minusMinutes(5))

        claimed.shouldBeNull()
    }

    @Test
    fun `failActiveJobs marks IN_PROGRESS job as FAILED`() {
        val inProgress = dao.save(createJob(status = SyncStatus.IN_PROGRESS))
        dao.save(createJob(status = SyncStatus.COMPLETED))

        val count = dao.failActiveJobs("shutdown")

        count shouldBe 1
        entityManager.clear()
        dao.findById(inProgress.id!!)!!.status shouldBe SyncStatus.FAILED
    }

    @Test
    fun `failActiveJobs returns zero when no active jobs exist`() {
        dao.save(createJob(status = SyncStatus.COMPLETED))
        dao.save(createJob(status = SyncStatus.FAILED))

        val count = dao.failActiveJobs("shutdown")

        count shouldBe 0
    }

    @Test
    fun `tryCreateJob creates pending job when no active jobs exist`() {
        val created = dao.tryCreateJob(SyncJob.Type.INCREMENTAL)

        created.shouldNotBeNull()
        created.type shouldBe SyncJob.Type.INCREMENTAL
        created.status shouldBe SyncStatus.PENDING
        created.id.shouldNotBeNull()
    }

    @Test
    fun `tryCreateJob returns null when a pending job already exists even for different type`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.PENDING))

        val created = dao.tryCreateJob(SyncJob.Type.INCREMENTAL)

        created.shouldBeNull()
    }

    @Test
    fun `tryCreateJob returns null when an in progress job already exists even for different type`() {
        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.IN_PROGRESS))

        val created = dao.tryCreateJob(SyncJob.Type.INCREMENTAL)

        created.shouldBeNull()
    }

    @Test
    fun `touchUpdatedAt updates only updatedAt timestamp`() {
        val saved = dao.save(createJob(status = SyncStatus.IN_PROGRESS))
        val staleTime = now.minusHours(3)
        jdbcTemplate.update("UPDATE sync_jobs SET updated_at = ? WHERE id = ?", staleTime, saved.id)

        dao.touchUpdatedAt(saved.id!!)

        val found = dao.findById(saved.id!!).shouldNotBeNull()
        found.status shouldBe SyncStatus.IN_PROGRESS
        found.updatedAt.isAfter(staleTime) shouldBe true
    }

    @Test
    fun `hasCompletedFullSync returns true only when a completed full sync exists`() {
        dao.hasCompletedFullSync() shouldBe false

        dao.save(createJob(type = SyncJob.Type.INCREMENTAL, status = SyncStatus.COMPLETED))
        dao.hasCompletedFullSync() shouldBe false

        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.FAILED))
        dao.hasCompletedFullSync() shouldBe false

        dao.save(createJob(type = SyncJob.Type.FULL, status = SyncStatus.COMPLETED))
        dao.hasCompletedFullSync() shouldBe true
    }

    @Test
    fun `findLatestCompleted returns latest completed job only`() {
        dao.save(createJob(status = SyncStatus.COMPLETED, startedAt = now.minusHours(3)))
        dao.save(createJob(status = SyncStatus.FAILED, startedAt = now.minusHours(2)))
        val latestCompleted = dao.save(
            createJob(type = SyncJob.Type.INCREMENTAL, status = SyncStatus.COMPLETED, startedAt = now),
        )

        val found = dao.findLatestCompleted()

        found.shouldNotBeNull()
        found.id shouldBe latestCompleted.id
        found.type shouldBe SyncJob.Type.INCREMENTAL
        found.status shouldBe SyncStatus.COMPLETED
    }
}
