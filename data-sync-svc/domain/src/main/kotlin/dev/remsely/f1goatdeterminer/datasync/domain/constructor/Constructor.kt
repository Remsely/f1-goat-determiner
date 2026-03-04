package dev.remsely.f1goatdeterminer.datasync.domain.constructor

data class Constructor(
    val id: Int? = null,
    val ref: String,
    val name: String,
    val nationality: String?,
)
