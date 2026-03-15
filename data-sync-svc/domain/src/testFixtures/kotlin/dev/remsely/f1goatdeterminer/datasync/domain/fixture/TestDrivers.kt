package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import java.time.LocalDate

object TestDrivers {
    val hamilton
        get() = Driver(
            ref = "hamilton",
            number = 44,
            code = "HAM",
            forename = "Lewis",
            surname = "Hamilton",
            dateOfBirth = LocalDate.of(1985, 1, 7),
            nationality = "British",
        )

    val verstappen
        get() = Driver(
            ref = "verstappen",
            number = 1,
            code = "VER",
            forename = "Max",
            surname = "Verstappen",
            dateOfBirth = LocalDate.of(1997, 9, 30),
            nationality = "Dutch",
        )
}

