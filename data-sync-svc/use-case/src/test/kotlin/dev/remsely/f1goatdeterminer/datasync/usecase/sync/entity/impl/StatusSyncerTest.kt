package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class StatusSyncerTest {

    private val statusFetcher = mockk<F1StatusFetcher>()
    private val statusPersister = mockk<StatusPersister>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = StatusSyncer(statusFetcher, statusPersister, checkpointPersister, txHelper)

    @Test
    fun `syncs statuses from fetcher and persists them`() {
        val statuses = listOf(
            Status(id = 1, status = "Finished"),
            Status(id = 2, status = "+1 Lap"),
        )

        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Status>) -> Unit>()
            callback(PageFetchResult(statuses, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 2)
        }
        every { statusPersister.upsertAll(statuses) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastOffset shouldBe 100
        verify { statusPersister.upsertAll(statuses) }
    }

    @Test
    fun `returns zero records when fetcher returns empty pages`() {
        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `accumulates records across multiple pages`() {
        val page1 = listOf(Status(id = 1, status = "Finished"))
        val page2 = listOf(Status(id = 2, status = "+1 Lap"))

        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Status>) -> Unit>()
            callback(PageFetchResult(page1, pageNumber = 1, totalPages = 2, nextOffset = 100))
            callback(PageFetchResult(page2, pageNumber = 2, totalPages = 2, nextOffset = 200))
            PaginationSummary(apiCalls = 2)
        }
        every { statusPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastOffset shouldBe 200
        verify(exactly = 2) { statusPersister.upsertAll(any()) }
    }

    @Test
    fun `resumes from checkpoint offset`() {
        val statuses = listOf(Status(id = 3, status = "Accident"))

        every { statusFetcher.forEachPageOfStatuses(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<Status>) -> Unit>()
            callback(PageFetchResult(statuses, pageNumber = 2, totalPages = 2, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { statusPersister.upsertAll(statuses) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 5,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 6
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
    }

    @Test
    fun `updates checkpoint progress after each page`() {
        val statuses = listOf(Status(id = 1, status = "Finished"))

        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Status>) -> Unit>()
            callback(PageFetchResult(statuses, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { statusPersister.upsertAll(statuses) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        syncer.sync(checkpoint)

        verify {
            checkpointPersister.updateProgress(
                id = 1L,
                lastOffset = 100,
                recordsSynced = 1,
            )
        }
    }

    @Test
    fun `skips persist when page items are empty`() {
        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Status>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { statusPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.STATUSES
    }

    @Test
    fun `returns null for season and round`() {
        every { statusFetcher.forEachPageOfStatuses(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.lastSeason shouldBe null
        result.lastRound shouldBe null
    }
}
