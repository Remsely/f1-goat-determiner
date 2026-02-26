package dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor

import dev.remsely.f1goatdeterminer.datasync.jolpica.config.JolpicaClientProperties
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RateLimitInterceptor(
    properties: JolpicaClientProperties,
) : ClientHttpRequestInterceptor {

    private val rateLimiter: RateLimiter = RateLimiter.of(
        "jolpica",
        RateLimiterConfig.custom()
            .limitForPeriod(properties.rateLimit)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofSeconds(30))
            .build(),
    )

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        RateLimiter.waitForPermission(rateLimiter)
        return execution.execute(request, body)
    }
}
