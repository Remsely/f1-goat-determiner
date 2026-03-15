package dev.remsely.f1goatdeterminer.datasync.scheduled

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.time.Duration

class ScheduledModuleContextTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)
        .withPropertyValues(
            "sync.schedule.resume-on-startup=false",
            "sync.schedule.full-on-first-run=false",
            "sync.schedule.incremental-cron=0 30 * * * *",
            "sync.schedule.incremental-lock-at-most-for=20m",
            "sync.schedule.incremental-lock-at-least-for=5s",
            "sync.schedule.stale-job-threshold=10m",
        )

    @Test
    fun `context binds sync scheduler properties`() {
        contextRunner.run { context ->
            val properties = context.getBean<SyncSchedulerProperties>()

            properties.resumeOnStartup shouldBe false
            properties.fullOnFirstRun shouldBe false
            properties.incrementalCron shouldBe "0 30 * * * *"
            properties.incrementalLockAtMostFor shouldBe Duration.ofMinutes(20)
            properties.incrementalLockAtLeastFor shouldBe Duration.ofSeconds(5)
            properties.staleJobThreshold shouldBe Duration.ofMinutes(10)
        }
    }

    @Configuration
    @EnableConfigurationProperties(SyncSchedulerProperties::class)
    class TestConfig
}
