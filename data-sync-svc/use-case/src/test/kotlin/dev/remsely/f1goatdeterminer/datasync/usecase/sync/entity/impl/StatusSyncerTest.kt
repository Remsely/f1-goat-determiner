package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity.impl

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status
import dev.remsely.f1goatdeterminer.datasync.domain.result.status.StatusPersister
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint.SyncCheckpoint
import dev.remsely.f1goatdeterminer.datasync.usecase.port.F1StatusFetcher
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class StatusSyncerTest {

    private val statusFetcher = mockk<F1StatusFetcher>()
    private val statusPersister = mockk<StatusPersister>()

    private val syncer = StatusSyncer(statusFetcher, statusPersister)

    @Test
    fun `syncs statuses from fetcher and persists them`() {
        val statuses = listOf(
            Status(id = 1, status = "Finished"),
            Status(id = 2, status = "+1 Lap"),
        )

        every { statusFetcher.fetchAll(0) } returns statuses
        every { statusPersister.upsertAll(statuses) } returns 2

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 2
        result.apiCallsMade shouldBe 1
        result.lastOffset shouldBe 2
        verify { statusPersister.upsertAll(statuses) }
    }

    @Test
    fun `returns zero records when fetcher returns empty list`() {
        every { statusFetcher.fetchAll(0) } returns emptyList()

        val checkpoint = SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES).copy(id = 1L)
        val result = syncer.sync(checkpoint)

        result.recordsSynced shouldBe 0
        result.apiCallsMade shouldBe 1
    }
}
