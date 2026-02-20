package dev.remsely.f1goatdeterminer.datasync.domain.result.qualifying

data class QualifyingResult(
    val id: Int,
    val grandPrixId: Int,
    val driverId: Int,
    val constructorId: Int,
    val number: Int?,
    val position: Int,
    val q1: String?,
    val q2: String?,
    val q3: String?,
) {
    val isPole: Boolean
        get() = position == 1
}
