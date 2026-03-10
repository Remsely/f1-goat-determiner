package dev.remsely.f1goatdeterminer.datasync.usecase.sync.entity

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Helper service that provides transactional boundaries for batch persistence operations.
 *
 * This is needed because seasonal syncers (RaceResult, Qualifying, Standings) process
 * data round-by-round. Each round should be persisted in its own transaction, so that
 * a failure on round N does not roll back rounds 1..N-1.
 *
 * Without this, putting @Transactional on the syncer's sync() method would create a single
 * transaction spanning potentially hundreds of HTTP calls + DB writes, which:
 * - Holds a DB connection for the entire duration
 * - Rolls back ALL progress on any failure
 * - Makes checkpoint-based resume useless
 */
@Service
class TransactionalPersistenceHelper {

    /**
     * Executes the given [block] within a new transaction.
     * Returns the result of the block (typically the number of upserted records).
     */
    @Transactional
    fun <T> executeInTransaction(block: () -> T): T = block()
}
