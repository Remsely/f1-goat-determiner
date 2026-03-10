package dev.remsely.f1goatdeterminer.datasync.scheduled

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "sync.schedule")
data class SyncSchedulerProperties(
    val resumeOnStartup: Boolean,
    val fullOnFirstRun: Boolean,
    val incrementalCron: String,
    val incrementalLockAtMostFor: Duration,
    val incrementalLockAtLeastFor: Duration,
    val staleJobThreshold: Duration,
)
