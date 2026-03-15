package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1ConstructorFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ConstructorSyncerTest {

    private val constructorFetcher = mockk<F1ConstructorFetcher>()
    private val constructorPersister = mockk<ConstructorPersister>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = ConstructorSyncer(
        constructorFetcher,
        constructorPersister,
        checkpointPersister,
        txHelper,
    )

    @Test
    fun `syncs constructors from fetcher and persists them`() {
        val constructors = listOf(
            Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"),
            Constructor(ref = "mclaren", name = "McLaren", nationality = "British"),
        )

        every { constructorFetcher.forEachPageOfConstructors(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Constructor>) -> Unit>()
            callback(PageFetchResult(constructors, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { constructorPersister.upsertAll(constructors) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
        result.lastSeason shouldBe null
        result.lastRound shouldBe null
        verify { constructorPersister.upsertAll(constructors) }
    }

    @Test
    fun `returns zero records when fetcher returns empty pages`() {
        every { constructorFetcher.forEachPageOfConstructors(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `accumulates records across multiple pages`() {
        val page1 = listOf(Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"))
        val page2 = listOf(Constructor(ref = "mclaren", name = "McLaren", nationality = "British"))

        every { constructorFetcher.forEachPageOfConstructors(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Constructor>) -> Unit>()
            callback(PageFetchResult(page1, pageNumber = 1, totalPages = 2, nextOffset = 100))
            callback(PageFetchResult(page2, pageNumber = 2, totalPages = 2, nextOffset = 200))
            PaginationSummary(apiCalls = 2)
        }
        every { constructorPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastOffset shouldBe 200
        verify(exactly = 2) { constructorPersister.upsertAll(any()) }
    }

    @Test
    fun `resumes from checkpoint offset`() {
        val constructors = listOf(Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"))

        every { constructorFetcher.forEachPageOfConstructors(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<Constructor>) -> Unit>()
            callback(PageFetchResult(constructors, pageNumber = 2, totalPages = 2, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { constructorPersister.upsertAll(constructors) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(
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
        val constructors = listOf(Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian"))

        every { constructorFetcher.forEachPageOfConstructors(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Constructor>) -> Unit>()
            callback(PageFetchResult(constructors, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { constructorPersister.upsertAll(constructors) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(id = 1L)
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
        every { constructorFetcher.forEachPageOfConstructors(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Constructor>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CONSTRUCTORS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { constructorPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.CONSTRUCTORS
    }
}
