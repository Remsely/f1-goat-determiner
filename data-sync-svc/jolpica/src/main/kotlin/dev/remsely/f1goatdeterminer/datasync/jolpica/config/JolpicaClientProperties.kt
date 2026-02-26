package dev.remsely.f1goatdeterminer.datasync.jolpica.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jolpica")
data class JolpicaClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val pageSize: Int,
    val rateLimit: Int,
    val retryMaxAttempts: Int,
    val retryWaitDuration: Duration,
)
