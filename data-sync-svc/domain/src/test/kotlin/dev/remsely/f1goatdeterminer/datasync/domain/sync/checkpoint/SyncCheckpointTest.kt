package dev.remsely.f1goatdeterminer.datasync.domain.sync.checkpoint

import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncEntityType
import dev.remsely.f1goatdeterminer.datasync.domain.sync.SyncStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SyncCheckpointTest {

    @Test
    fun `initPending creates pending checkpoint with empty progress`() {
        val checkpoint = SyncCheckpoint.initPending(jobId = 42L, entityType = SyncEntityType.RACE_RESULTS)

        checkpoint shouldBe SyncCheckpoint(
            id = null,
            jobId = 42L,
            entityType = SyncEntityType.RACE_RESULTS,
            status = SyncStatus.PENDING,
            lastOffset = 0,
            lastSeason = null,
            lastRound = null,
            recordsSynced = 0,
            startedAt = null,
            completedAt = null,
            errorMessage = null,
            retryCount = 0,
        )
    }

    @Test
    fun `initFromPrevious keeps cursor fields and resets execution state`() {
        val previous = SyncCheckpoint(
            id = 7L,
            jobId = 1L,
            entityType = SyncEntityType.DRIVER_STANDINGS,
            status = SyncStatus.COMPLETED,
            lastOffset = 300,
            lastSeason = 2024,
            lastRound = 12,
            recordsSynced = 999,
            startedAt = LocalDateTime.of(2024, 7, 1, 12, 0),
            completedAt = LocalDateTime.of(2024, 7, 1, 12, 5),
            errorMessage = "old error",
            retryCount = 2,
        )

        val next = SyncCheckpoint.initFromPrevious(jobId = 2L, previous = previous)

        next shouldBe SyncCheckpoint(
            id = null,
            jobId = 2L,
            entityType = SyncEntityType.DRIVER_STANDINGS,
            status = SyncStatus.PENDING,
            lastOffset = 300,
            lastSeason = 2024,
            lastRound = 12,
            recordsSynced = 0,
            startedAt = null,
            completedAt = null,
            errorMessage = null,
            retryCount = 0,
        )
    }

    @Test
    fun `isCompleted is true only for completed status`() {
        SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES)
            .copy(status = SyncStatus.COMPLETED).isCompleted shouldBe true
        SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES)
            .copy(status = SyncStatus.PENDING).isCompleted shouldBe false
        SyncCheckpoint.initPending(1L, SyncEntityType.STATUSES)
            .copy(status = SyncStatus.FAILED).isCompleted shouldBe false
    }

    @Test
    fun `isPending is true only for pending status`() {
        SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS).isPending shouldBe true
        SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS)
            .copy(status = SyncStatus.COMPLETED).isPending shouldBe false
        SyncCheckpoint.initPending(1L, SyncEntityType.CIRCUITS)
            .copy(status = SyncStatus.FAILED).isPending shouldBe false
    }

    @Test
    fun `isRetryable is true only for failed checkpoints below max retries`() {
        SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX)
            .copy(status = SyncStatus.FAILED, retryCount = SyncCheckpoint.MAX_RETRIES - 1)
            .isRetryable shouldBe true

        SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX)
            .copy(status = SyncStatus.FAILED, retryCount = SyncCheckpoint.MAX_RETRIES)
            .isRetryable shouldBe false

        SyncCheckpoint.initPending(1L, SyncEntityType.GRAND_PRIX)
            .copy(status = SyncStatus.COMPLETED, retryCount = SyncCheckpoint.MAX_RETRIES - 1)
            .isRetryable shouldBe false
    }
}
