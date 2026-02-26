package dev.remsely.f1goatdeterminer.datasync.db.entity.driver

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver

fun DriverEntity.toDomain() = Driver(
    id = id,
    ref = ref,
    number = number,
    code = code,
    forename = forename,
    surname = surname,
    dateOfBirth = dob,
    nationality = nationality,
)

fun List<DriverEntity>.toDomain(): List<Driver> = map { it.toDomain() }
