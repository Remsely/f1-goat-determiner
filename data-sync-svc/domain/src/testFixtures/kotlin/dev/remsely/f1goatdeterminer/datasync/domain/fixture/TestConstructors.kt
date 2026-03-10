package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.constructor.Constructor

object TestConstructors {
    val ferrari
        get() = Constructor(
            ref = "ferrari",
            name = "Ferrari",
            nationality = "Italian",
        )

    val mercedes
        get() = Constructor(
            ref = "mercedes",
            name = "Mercedes",
            nationality = "German",
        )
}

