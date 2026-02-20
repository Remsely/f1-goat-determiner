package dev.remsely.f1goatdeterminer.datasync.db.entity.driver

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver

fun Driver.toEntity() = DriverEntity(
    id = id,
    ref = ref,
    number = number,
    code = code,
    forename = forename,
    surname = surname,
    dob = dateOfBirth,
    nationality = nationality,
)

fun List<Driver>.toEntity(): List<DriverEntity> = map { it.toEntity() }
