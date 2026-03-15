package dev.remsely.f1goatdeterminer.datasync.domain.fixture

import dev.remsely.f1goatdeterminer.datasync.domain.result.status.Status

object TestStatuses {
    val finished
        get() = Status(id = 1, status = "Finished")
}

