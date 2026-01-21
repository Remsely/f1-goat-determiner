package dev.remsely.f1goatdeterminer.datasync.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DataSyncApplication

fun main(args: Array<String>) {
    runApplication<DataSyncApplication>(*args)
}
