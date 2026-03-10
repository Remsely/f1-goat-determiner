package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.grandprix.GrandPrix
import java.time.LocalDate

object TestGrandPrix {
    fun italianGp2024(circuitId: Int) = GrandPrix(
        season = 2024,
        round = 1,
        circuitId = circuitId,
        name = "Italian GP",
        date = LocalDate.of(2024, 9, 1),
        time = null,
    )
}

