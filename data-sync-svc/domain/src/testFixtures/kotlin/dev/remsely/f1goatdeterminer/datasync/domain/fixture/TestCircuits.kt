package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.circuit.Circuit

object TestCircuits {
    val monza
        get() = Circuit(
            ref = "monza",
            name = "Autodromo Nazionale Monza",
            locality = "Monza",
            country = "Italy",
        )
}

