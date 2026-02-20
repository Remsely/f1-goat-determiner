package dev.remsely.f1goatdeterminer.datasync.domain.circuit

data class Circuit(
    val id: Int,
    val ref: String,
    val name: String,
    val locality: String?,
    val country: String?,
)
