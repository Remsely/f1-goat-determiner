package dev.remsely.f1goatdeterminer.datasync.jolpica.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "integration.jolpica")
data class JolpicaClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val pageSize: Int,
    val rateLimitRps: Double,
    val retryMaxAttempts: Int,
    val retryWaitDuration: Duration,
    val retryBackoffMultiplier: Double,
)
