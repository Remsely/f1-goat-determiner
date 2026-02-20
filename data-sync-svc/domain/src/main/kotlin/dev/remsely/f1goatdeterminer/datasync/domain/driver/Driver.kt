package dev.remsely.f1goatdeterminer.datasync.domain.driver

import java.time.LocalDate

data class Driver(
    val id: Int,
    val ref: String,
    val number: Int?,
    val code: String?,
    val forename: String,
    val surname: String,
    val dateOfBirth: LocalDate?,
    val nationality: String?,
) {
    val fullName: String
        get() = "$forename $surname"
}
