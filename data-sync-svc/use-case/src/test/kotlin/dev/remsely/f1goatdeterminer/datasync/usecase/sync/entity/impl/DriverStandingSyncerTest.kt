package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverFinder
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrixFinder
import dev.remsely.f1goatdeterminer.datasync.domain.standings.driver.DriverStandingPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverStandingFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.FetchedDriverStanding
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DriverStandingSyncerTest {

    private val driverStandingFetcher = mockk<F1DriverStandingFetcher>()
    private val driverStandingPersister = mockk<DriverStandingPersister>()
    private val driverFinder = mockk<DriverFinder>()
    private val grandPrixFinder = mockk<GrandPrixFinder>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = DriverStandingSyncer(
        driverStandingFetcher,
        driverStandingPersister,
        driverFinder,
        grandPrixFinder,
        checkpointPersister,
        txHelper,
    )

    @Test
    fun `syncs standings across multiple seasons`() {
        every { grandPrixFinder.findAllSeasons() } returns listOf(2023, 2024)
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (2023 to 1) to 1,
            (2024 to 1) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("leclerc" to 1)

        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2023, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2023, 1)), 1, 1, 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2024, 1)), 1, 1, 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverStandingPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVER_STANDINGS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastSeason shouldBe 2024
        verify(exactly = 2) { driverStandingPersister.upsertAll(any()) }
    }

    @Test
    fun `resumes from checkpoint lastSeason skipping earlier seasons`() {
        every { grandPrixFinder.findAllSeasons() } returns listOf(2023, 2024)
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((2024 to 1) to 2)
        every { driverFinder.findAllRefToId() } returns mapOf("leclerc" to 1)

        // Standings always re-fetch from offset 0 because Jolpica returns only the latest round
        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2024, 1)), 1, 1, 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverStandingPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVER_STANDINGS).copy(
            id = 1L,
            lastSeason = 2024,
            lastOffset = 50,
            recordsSynced = 5,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 6
        result.apiCallsMade shouldBe 1
        result.lastSeason shouldBe 2024
        verify(exactly = 0) { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2023, any(), any()) }
    }

    @Test
    fun `returns zero records when no seasons exist`() {
        every { grandPrixFinder.findAllSeasons() } returns emptyList()
        every { grandPrixFinder.findAllSeasonRoundToId() } returns emptyMap()
        every { driverFinder.findAllRefToId() } returns emptyMap()

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVER_STANDINGS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 0
    }

    @Test
    fun `accumulates API calls across seasons`() {
        every { grandPrixFinder.findAllSeasons() } returns listOf(2023, 2024)
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf(
            (2023 to 1) to 1,
            (2024 to 1) to 2,
        )
        every { driverFinder.findAllRefToId() } returns mapOf("leclerc" to 1)

        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2023, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2023, 1)), 1, 2, 100))
            callback(PageFetchResult(listOf(testStanding(2023, 1)), 2, 2, 200))
            PaginationSummary(apiCalls = 2)
        }
        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2024, 1)), 1, 2, 100))
            callback(PageFetchResult(listOf(testStanding(2024, 1)), 2, 2, 200))
            PaginationSummary(apiCalls = 2)
        }
        every { driverStandingPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVER_STANDINGS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.apiCallsMade shouldBe 4
        result.recordsSynced shouldBe 4
    }

    @Test
    fun `handles empty standings for a season`() {
        every { grandPrixFinder.findAllSeasons() } returns listOf(2023, 2024)
        every { grandPrixFinder.findAllSeasonRoundToId() } returns mapOf((2024 to 1) to 2)
        every { driverFinder.findAllRefToId() } returns mapOf("leclerc" to 1)

        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2023, 0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }
        every { driverStandingFetcher.forEachPageOfSeasonDriverStandings(2024, 0, any()) } answers {
            val callback = thirdArg<(PageFetchResult<FetchedDriverStanding>) -> Unit>()
            callback(PageFetchResult(listOf(testStanding(2024, 1)), 1, 1, 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverStandingPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVER_STANDINGS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 1
        result.apiCallsMade shouldBe 2
    }

    private fun testStanding(season: Int, round: Int) = FetchedDriverStanding(
        season = season,
        round = round,
        driverRef = "leclerc",
        points = BigDecimal("25.0"),
        position = 1,
        positionText = "1",
        wins = 1,
    )
}
