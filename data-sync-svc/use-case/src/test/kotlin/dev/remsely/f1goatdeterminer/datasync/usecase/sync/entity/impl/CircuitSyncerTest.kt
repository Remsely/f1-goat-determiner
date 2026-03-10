package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1CircuitFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class CircuitSyncerTest {

    private val circuitFetcher = mockk<F1CircuitFetcher>()
    private val circuitPersister = mockk<CircuitPersister>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = CircuitSyncer(circuitFetcher, circuitPersister, checkpointPersister, txHelper)

    @Test
    fun `syncs circuits from fetcher and persists them`() {
        val circuits = listOf(
            Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"),
            Circuit(ref = "spa", name = "Spa-Francorchamps", locality = "Spa", country = "Belgium"),
        )

        every { circuitFetcher.forEachPageOfCircuits(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Circuit>) -> Unit>()
            callback(PageFetchResult(circuits, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { circuitPersister.upsertAll(circuits) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
        result.lastSeason shouldBe null
        result.lastRound shouldBe null
        verify { circuitPersister.upsertAll(circuits) }
    }

    @Test
    fun `returns zero records when fetcher returns empty pages`() {
        every { circuitFetcher.forEachPageOfCircuits(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `accumulates records across multiple pages`() {
        val page1 = listOf(Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"))
        val page2 = listOf(Circuit(ref = "spa", name = "Spa", locality = "Spa", country = "Belgium"))

        every { circuitFetcher.forEachPageOfCircuits(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Circuit>) -> Unit>()
            callback(PageFetchResult(page1, pageNumber = 1, totalPages = 2, nextOffset = 100))
            callback(PageFetchResult(page2, pageNumber = 2, totalPages = 2, nextOffset = 200))
            PaginationSummary(apiCalls = 2)
        }
        every { circuitPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastOffset shouldBe 200
        verify(exactly = 2) { circuitPersister.upsertAll(any()) }
    }

    @Test
    fun `resumes from checkpoint offset`() {
        val circuits = listOf(Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"))

        every { circuitFetcher.forEachPageOfCircuits(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<Circuit>) -> Unit>()
            callback(PageFetchResult(circuits, pageNumber = 2, totalPages = 2, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { circuitPersister.upsertAll(circuits) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 3,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 4
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
    }

    @Test
    fun `updates checkpoint progress after each page`() {
        val circuits = listOf(Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy"))

        every { circuitFetcher.forEachPageOfCircuits(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Circuit>) -> Unit>()
            callback(PageFetchResult(circuits, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { circuitPersister.upsertAll(circuits) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L)
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
        every { circuitFetcher.forEachPageOfCircuits(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Circuit>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { circuitPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.CIRCUITS
    }
}
