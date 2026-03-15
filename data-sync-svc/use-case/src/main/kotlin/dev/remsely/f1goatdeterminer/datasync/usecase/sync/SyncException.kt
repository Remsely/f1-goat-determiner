package dev.remsely.f1goatdeterminer.datasync.usecase.sync

/**
 * Exception thrown during entity synchronization.
 * Wraps the underlying cause with additional context.
 */
class SyncException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
