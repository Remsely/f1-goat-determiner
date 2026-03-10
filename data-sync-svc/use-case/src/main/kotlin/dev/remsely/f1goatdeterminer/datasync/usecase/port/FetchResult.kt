package dev.remsely.f1goatdeterminer.datasync.usecase.port

/**
 * Result of a single page fetched from an external API.
 *
 * @param T the type of the fetched items
 * @param items items from this page
 * @param pageNumber 1-based page number
 * @param totalPages estimated total number of pages
 */
data class PageFetchResult<T>(
    val items: List<T>,
    val pageNumber: Int,
    val totalPages: Int,
    val nextOffset: Int,
)

/**
 * Summary of a paginated fetch operation that was processed page-by-page.
 */
data class PaginationSummary(
    val apiCalls: Int,
)
