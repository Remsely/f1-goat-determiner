package dev.remsely.f1goatdeterminer.datasync.usecase.sync

/**
 * Thrown when all retry attempts have been exhausted due to HTTP 429 (Too Many Requests).
 * Signals that the entire sync job should be stopped immediately.
 */
class RateLimitExhaustedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
