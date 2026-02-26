package dev.remsely.f1goatdeterminer.datasync.jolpica.mapper

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.jolpica.dto.DriverDto
import java.time.LocalDate

fun DriverDto.toDomain(id: Int): Driver = Driver(
    id = id,
    ref = driverId,
    number = permanentNumber?.toIntOrNull(),
    code = code,
    forename = givenName,
    surname = familyName,
    dateOfBirth = dateOfBirth?.let { LocalDate.parse(it) },
    nationality = nationality,
)
