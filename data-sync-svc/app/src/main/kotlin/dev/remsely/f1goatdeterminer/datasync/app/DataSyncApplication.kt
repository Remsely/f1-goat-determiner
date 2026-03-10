package dev.remsely.f1goatdeterminer.datasync.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["dev.remsely.f1goatdeterminer.datasync"])
@EntityScan(basePackages = ["dev.remsely.f1goatdeterminer.datasync.db.entity"])
@EnableJpaRepositories(basePackages = ["dev.remsely.f1goatdeterminer.datasync.db.repository"])
@ConfigurationPropertiesScan(basePackages = ["dev.remsely.f1goatdeterminer.datasync"])
@EnableScheduling
class DataSyncApplication

fun main() {
    runApplication<DataSyncApplication>()
}
