package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.ConstructorFinder
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.result.race.RaceResultPersister
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusFinder
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1RaceResultFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedRaceResult
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
import java.math.BigDecimal

class RaceResultSyncerTest {

    private val raceResultFetcher = mockk<F1RaceResultFetcher>()
    private val raceResultPersister = mockk<RaceResultPersister>()
    private val grandPrixFinder = mockk<GrandPrixFinder>()
    private val driverFinder = mockk<DriverFinder>()
    private val constructorFinder = mockk<ConstructorFinder>()
    private val statusFinder = mockk<StatusFinder>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = RaceResultSyncer(
        raceResultFetcher,
        raceResultPersister,
        grandPrixFinder,
        driverFinder,
        constructorFinder,
        statusFinder,
        checkpointPersister,
        txHelper,
    )

    @Test
    fun `syncs all race results in a single page`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (1950 to 1) to 1,
            (1950 to 2) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1), testFetchedResult(1950, 2)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { raceResultPersister.upsertAll(any()) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastSeason shouldBe 1950
        result.lastRound shouldBe 2
        verify(exactly = 1) { raceResultPersister.upsertAll(any()) }
    }

    @Test
    fun `persists and logs after each page`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (1950 to 1) to 1,
            (1951 to 1) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 1,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1951, 1)),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 200,
                ),
            )
            PaginationSummary(apiCalls = 2)
        }
        every { raceResultPersister.upsertAll(any()) } returnsMany listOf(1, 1)

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastSeason shouldBe 1951
        verify(exactly = 2) { raceResultPersister.upsertAll(any()) }
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
    fun `returns empty result when no data fetched`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns emptyMap()
        every { statusFinder.findAllStatusToId() } returns emptyMap()

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `resumes from checkpoint offset`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((1950 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 2,
                    totalPages = 2,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }
        every { raceResultPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 10,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 11
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `throws when grand prix lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "GrandPrix not found"
    }

    @Test
    fun `throws when driver lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((1950 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "Driver not found"
    }

    @Test
    fun `throws when constructor lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((1950 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns emptyMap()
        every { statusFinder.findAllStatusToId() } returns mapOf("Finished" to 1)

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "Constructor not found"
    }

    @Test
    fun `throws when status lookup fails`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((1950 to 1) to 1)
        every { driverFinder.findAllRefToId() } returns mapOf("farina" to 1)
        every { constructorFinder.findAllRefToId() } returns mapOf("alfa" to 1)
        every { statusFinder.findAllStatusToId() } returns emptyMap()

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(
                PageFetchResult(
                    items = listOf(testFetchedResult(1950, 1)),
                    pageNumber = 1,
                    totalPages = 1,
                    nextOffset = 100,
                ),
            )
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)

        val exception = shouldThrow<IllegalStateException> { syncer.sync(checkpoint) }
        exception.message shouldContain "Status not found"
    }

    @Test
    fun `skips persist when page items are empty`() {
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns emptyMap()
        every { constructorFinder.findAllRefToId() } returns emptyMap()
        every { statusFinder.findAllStatusToId() } returns emptyMap()

        every { raceResultFetcher.forEachPageOfResults(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<FetchedRaceResult>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.RACE_RESULTS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { raceResultPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.RACE_RESULTS
    }

    private fun testFetchedResult(season: Int, round: Int) = FetchedRaceResult(
        season = season,
        round = round,
        driverRef = "farina",
        constructorRef = "alfa",
        statusText = "Finished",
        number = 1,
        grid = 1,
        position = 1,
        positionText = "1",
        positionOrder = 1,
        points = BigDecimal("8.0"),
        laps = 62,
        time = "2:13:23.6",
        milliseconds = null,
        fastestLap = null,
        fastestLapRank = null,
        fastestLapTime = null,
        fastestLapSpeed = null,
    )
}
