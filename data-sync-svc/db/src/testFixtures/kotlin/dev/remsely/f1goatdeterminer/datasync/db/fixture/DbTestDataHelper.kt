package dev.remsely.f1goatdeterminer.datasync.db.fixture

import dev.remsely.f1goatdeterminer.datasync.db.repository.circuit.CircuitDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.constructor.ConstructorDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.driver.DriverDao
import dev.remsely.f1goatdeterminer.datasync.db.repository.grandprix.GrandPrixDao
import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit
import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor
import dev.remsely.f1goatdeterminer.datasync.domain.driver.Driver
import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Helper for inserting FK-referenced test data in integration tests.
 *
 * Avoids duplicating circuit/driver/constructor/grandprix setup
 * across multiple DAO tests that need these as foreign-key parents.
 */
@Component
class DbTestDataHelper(
    private val circuitDao: CircuitDao,
    private val driverDao: DriverDao,
    private val constructorDao: ConstructorDao,
    private val grandPrixDao: GrandPrixDao,
) {
    fun insertMonzaCircuit(): Int {
        circuitDao.upsertAll(
            listOf(Circuit(ref = "monza", name = "Monza", locality = "Monza", country = "Italy")),
        )
        return circuitDao.findIdByRef("monza")!!
    }

    fun insertHamiltonDriver(): Int {
        driverDao.upsertAll(
            listOf(
                Driver(
                    ref = "hamilton",
                    number = 44,
                    code = "HAM",
                    forename = "Lewis",
                    surname = "Hamilton",
                    dateOfBirth = LocalDate.of(1985, 1, 7),
                    nationality = "British",
                ),
            ),
        )
        return driverDao.findIdByRef("hamilton")!!
    }

    fun insertMercedesConstructor(): Int {
        constructorDao.upsertAll(
            listOf(Constructor(ref = "mercedes", name = "Mercedes", nationality = "German")),
        )
        return constructorDao.findIdByRef("mercedes")!!
    }

    fun insertFerrariConstructor(): Int {
        constructorDao.upsertAll(
            listOf(Constructor(ref = "ferrari", name = "Ferrari", nationality = "Italian")),
        )
        return constructorDao.findIdByRef("ferrari")!!
    }


    fun insertItalianGp2024(circuitId: Int): Int {
        grandPrixDao.upsertAll(
            listOf(
                GrandPrix(
                    season = 2024,
                    round = 1,
                    circuitId = circuitId,
                    name = "Italian GP",
                    date = LocalDate.of(2024, 9, 1),
                    time = null,
                ),
            ),
        )
        return grandPrixDao.findAllSeasonRoundToId()[2024 to 1]!!
    }
}

