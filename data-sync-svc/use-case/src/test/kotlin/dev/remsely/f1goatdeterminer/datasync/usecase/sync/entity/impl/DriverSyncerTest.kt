package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.domain.driver.DriverPersister
import dev.remsely.f1goatdeterminer.datasync.domain.fixture.TestDrivers
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpointPersister
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1DriverFetcher
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PageFetchResult
import dev.remsely.f1goatdeterminer.datasync.usecase.port.PaginationSummary
import dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.TransactionalPersistenceHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class DriverSyncerTest {

    private val driverFetcher = mockk<F1DriverFetcher>()
    private val driverPersister = mockk<DriverPersister>()
    private val checkpointPersister = mockk<SyncCheckpointPersister>(relaxed = true)
    private val txHelper = TransactionalPersistenceHelper()

    private val syncer = DriverSyncer(driverFetcher, driverPersister, checkpointPersister, txHelper)

    @Test
    fun `syncs drivers from fetcher and persists them`() {
        val drivers = listOf(
            TestDrivers.hamilton,
            TestDrivers.verstappen,
        )

        every { driverFetcher.forEachPageOfDrivers(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Driver>) -> Unit>()
            callback(PageFetchResult(drivers, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverPersister.upsertAll(drivers) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
        result.lastSeason shouldBe null
        result.lastRound shouldBe null
        verify { driverPersister.upsertAll(drivers) }
    }

    @Test
    fun `returns zero records when fetcher returns empty pages`() {
        every { driverFetcher.forEachPageOfDrivers(0, any()) } answers {
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }

    @Test
    fun `accumulates records across multiple pages`() {
        val page1 = listOf(TestDrivers.hamilton)
        val page2 = listOf(TestDrivers.verstappen)

        every { driverFetcher.forEachPageOfDrivers(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Driver>) -> Unit>()
            callback(PageFetchResult(page1, pageNumber = 1, totalPages = 2, nextOffset = 100))
            callback(PageFetchResult(page2, pageNumber = 2, totalPages = 2, nextOffset = 200))
            PaginationSummary(apiCalls = 2)
        }
        every { driverPersister.upsertAll(any()) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 2
        result.lastOffset shouldBe 200
        verify(exactly = 2) { driverPersister.upsertAll(any()) }
    }

    @Test
    fun `resumes from checkpoint offset`() {
        val drivers = listOf(TestDrivers.hamilton)

        every { driverFetcher.forEachPageOfDrivers(50, any()) } answers {
            val callback = secondArg<(PageFetchResult<Driver>) -> Unit>()
            callback(PageFetchResult(drivers, pageNumber = 2, totalPages = 2, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverPersister.upsertAll(drivers) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(
            id = 1L,
            lastOffset = 50,
            recordsSynced = 10,
        )
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 11
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 100
    }

    @Test
    fun `updates checkpoint progress after each page`() {
        val drivers = listOf(TestDrivers.hamilton)

        every { driverFetcher.forEachPageOfDrivers(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Driver>) -> Unit>()
            callback(PageFetchResult(drivers, pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }
        every { driverPersister.upsertAll(drivers) } returns 1

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 1L)
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
        every { driverFetcher.forEachPageOfDrivers(0, any()) } answers {
            val callback = secondArg<(PageFetchResult<Driver>) -> Unit>()
            callback(PageFetchResult(emptyList(), pageNumber = 1, totalPages = 1, nextOffset = 100))
            PaginationSummary(apiCalls = 1)
        }

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.DRIVERS).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        verify(exactly = 0) { driverPersister.upsertAll(any()) }
    }

    @Test
    fun `has correct entity type`() {
        syncer.entityType shouldBe SyncEntityType.DRIVERS
    }
}
