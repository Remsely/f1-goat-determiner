package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying.QualifyingResultPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1QualifyingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedQualifyingResult
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

class QualifyingSyncerTest {

    private val qualifyingFetcher = mockk<F1QualifyingFetcher>()
    private val qualifyingResultPersister = mockk<QualifyingResultPersister>()
    private val grandPrixFinder = mockk<GrandPrixFinder>()
    private val driverFinder = mockk<DriverFinder>()
    private val constructorFinder = mockk<ConstructorFinder>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = QualifyingSyncer(
        qualifyingFetcher,
        qualifyingResultPersister,
        grandPrixFinder,
        driverFinder,
        constructorFinder,
        checkpointPersister,
        txHelper,
    )

    @Test
    fun `syncs qualifying results from a single page`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (2023 to 1) to 1,
            (2023 to 2) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(
                        testQualifying(2023, 1),
                        testQualifying(2023, 2),
                    ),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { qualifyingResultPersister.upsertAll(any()) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
        result.lastSeason shouldBe 2023
        result.lastRound shouldBe 2
        verify(exactly = 1) { qualifyingResultPersister.upsertAll(any()) }
    }

    @Test
    fun `tracks last season and round correctly`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (2023 to 1) to 1,
            (2024 to 3) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(
                        testQualifying(2023, 1),
                        testQualifying(2024, 3),
                    ),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { qualifyingResultPersister.upsertAll(any()) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.lastSeason shouldBe 2024
        result.lastRound shouldBe 3
    }

    @Test
    fun `returns zero records when no data fetched`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns emptyMap()

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `resumes from checkpoint offset`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((2023 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2023, 1)),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { qualifyingResultPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 10,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 11
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `persists across multiple pages and updates checkpoint`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (2023 to 1) to 1,
            (2024 to 1) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2023, 1)),
                    pageNumber = 1,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2024, 1)),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 200,
                ),
            )
            PaginationSummary(apiCalls = 2)
        }
        every { qualifyingResultPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        verify(exactly = 2) { qualifyingResultPersister.upsertAll(any()) }
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
    fun `throws when grand prix lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2023, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "GrandPrix not found"
    }

    @Test
    fun `throws when driver lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((2023 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns mapOf("mercedes" to 1)

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2023, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "Driver not found"
    }

    @Test
    fun `throws when constructor lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((2023 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns mapOf("hamilton" to 1)
        every { constructorFinder.findAllRefToId() } returns emptyMap()

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testQualifying(2023, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "Constructor not found"
    }

    @Test
    fun `skips persist when page items are empty`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns emptyMap()

        every { qualifyingFetcher.forEachPageOfQualifying(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedQualifyingResult>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.QUALIFYING_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { qualifyingResultPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.QUALIFYING_RESULTS
    }

    private fun testQualifying(season: Int, round: Int) = FetchedQualifyingResult(
        season = season,
        round = round,
        driverRef = "hamilton",
        constructorRef = "mercedes",
        number = 44,
        position = 1,
        q1 = "1:23.456",
        q2 = "1:22.345",
        q3 = "1:21.234",
    )
}
