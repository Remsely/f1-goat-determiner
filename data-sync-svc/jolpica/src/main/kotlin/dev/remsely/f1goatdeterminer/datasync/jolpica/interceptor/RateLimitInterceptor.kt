package dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor

import dev.remsely.f1goatdeterminer.datasync.jolpica.config.JolpicaClientProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class RateLimitInterceptor(
    properties: JolpicaClientProperties,
) : ClientHttpRequestInterceptor {

    private val burstLimiter: RateLimiter = RateLimiter.of(
        "jolpica-burst",
        RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofMillis((MILLIS_PER_SECOND / properties.rateLimitRps).toLong()))
            .timeoutDuration(Duration.ofSeconds(BURST_TIMEOUT_SECONDS))
            .build(),
    )

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        RateLimiter.waitForPermission(burstLimiter)

        log.debug { ">> API ${request.method} ${request.uri}" }

        val response = execution.execute(request, body)

        log.debug { "<< API ${response.statusCode} ${request.uri}" }

        return response
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000.0
        const val BURST_TIMEOUT_SECONDS = 30L
    }
}
