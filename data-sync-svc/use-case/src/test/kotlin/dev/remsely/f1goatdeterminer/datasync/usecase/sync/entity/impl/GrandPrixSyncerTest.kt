package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.CircuitFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1GrandPrixFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedGrandPrix
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrandPrixSyncerTest {

    private val grandPrixFetcher = mockk<F1GrandPrixFetcher>()
    private val grandPrixPersister = mockk<GrandPrixPersister>()
    private val circuitFinder = mockk<CircuitFinder>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = GrandPrixSyncer(
        grandPrixFetcher,
        grandPrixPersister,
        circuitFinder,
        checkpointPersister,
        txHelper,
    )

    @Test
    fun `syncs grand prix data from a single page`() {
        every { circuitFinder.findAllRefToId() } returns mapOf("monza" to 1, "spa" to 2)

        every { grandPrixFetcher.forEachPageOfRaces(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedGrandPrix>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(
                        testGrandPrix(1950, 1, "monza"),
                        testGrandPrix(1950, 2, "spa"),
                    ),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { grandPrixPersister.upsertAll(any()) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
        result.lastSeason shouldBe 1950
        result.lastRound shouldBe 2
    }

    @Test
    fun `tracks last season and round across multiple seasons in a page`() {
        every { circuitFinder.findAllRefToId() } returns mapOf("monza" to 1)

        every { grandPrixFetcher.forEachPageOfRaces(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedGrandPrix>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(
                        testGrandPrix(1950, 1, "monza"),
                        testGrandPrix(1951, 1, "monza"),
                        testGrandPrix(1951, 3, "monza"),
                    ),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { grandPrixPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.lastSeason shouldBe 1951
        result.lastRound shouldBe 3
    }

    @Test
    fun `returns zero records when no data fetched`() {
        every { circuitFinder.findAllRefToId() } returns emptyMap()

        every { grandPrixFetcher.forEachPageOfRaces(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `resumes from checkpoint offset`() {
        every { circuitFinder.findAllRefToId() } returns mapOf("monza" to 1)

        every { grandPrixFetcher.forEachPageOfRaces(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedGrandPrix>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testGrandPrix(1951, 1, "monza")),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { grandPrixPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 5,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 6
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `persists across multiple pages and updates checkpoint`() {
        every { circuitFinder.findAllRefToId() } returns mapOf("monza" to 1, "spa" to 2)

        every { grandPrixFetcher.forEachPageOfRaces(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedGrandPrix>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testGrandPrix(1950, 1, "monza")),
                    pageNumber = 1,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            callback(
                PageFetchResult(
                    items = listOf(testGrandPrix(1951, 1, "spa")),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 200,
                ),
            )
            PaginationSummary(apiCalls = 2)
        }
        every { grandPrixPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        verify(exactly = 2) { grandPrixPersister.upsertAll(any()) }
        verify(exactly = 2) {
            checkpointPersister.updateProgress(
                id = any(),
                lastOffset = any(),
                lastSeason = any(),
                lastRound = any(),
                recordsSynced = any(),
            )
        }
    }

    @Test
    fun `throws when circuit ref is not found in lookup`() {
        every { circuitFinder.findAllRefToId() } returns mapOf("monza" to 1)

        every { grandPrixFetcher.forEachPageOfRaces(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedGrandPrix>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testGrandPrix(1950, 1, "unknown_circuit")),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "unknown_circuit"
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.GRAND_PRIX
    }

    private fun testGrandPrix(season: Int, round: Int, circuitRef: String) = FetchedGrandPrix(
        season = season,
        round = round,
        circuitRef = circuitRef,
        name = "Grand Prix $season-$round",
        date = LocalDate.of(season, 6, 15),
        time = null,
    )
}
