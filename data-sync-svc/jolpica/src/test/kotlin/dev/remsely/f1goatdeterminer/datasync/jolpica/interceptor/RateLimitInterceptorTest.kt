package dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor

import dev.remsely.f1goatdeterminer.datasync.jolpica.config.JolpicaClientProperties
import io.github.resilience4j.ratelimiter.RateLimiter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.util.ReflectionTestUtils
import java.net.URI
import java.time.Duration

class RateLimitInterceptorTest {

    @Test
    fun `intercept delegates request and returns response`() {
        val interceptor = RateLimitInterceptor(properties(rateLimitRps = 1000.0))
        val request = MockClientHttpRequest(HttpMethod.GET, URI("http://localhost/status.json"))
        val execution = mockk<ClientHttpRequestExecution>()
        val response = MockClientHttpResponse("{}".toByteArray(), HttpStatus.OK)

        every { execution.execute(any(), any()) } returns response

        val actual = interceptor.intercept(request, ByteArray(0), execution)

        actual.statusCode shouldBe HttpStatus.OK
        verify(exactly = 1) { execution.execute(request, match { it.isEmpty() }) }
    }

    @Test
    fun `interceptor config derives rate limiter timings from configured rps`() {
        val interceptor = RateLimitInterceptor(properties(rateLimitRps = 4.0))
        val limiter = ReflectionTestUtils.getField(interceptor, "burstLimiter") as RateLimiter

        limiter.rateLimiterConfig.limitForPeriod shouldBe 1
        limiter.rateLimiterConfig.limitRefreshPeriod shouldBe Duration.ofMillis(250)
        limiter.rateLimiterConfig.timeoutDuration shouldBe Duration.ofSeconds(30)
    }

    private fun properties(rateLimitRps: Double) = JolpicaClientProperties(
        baseUrl = "http://localhost",
        connectTimeout = Duration.ofSeconds(1),
        readTimeout = Duration.ofSeconds(1),
        pageSize = 100,
        rateLimitRps = rateLimitRps,
        retryMaxAttempts = 1,
        retryWaitDuration = Duration.ofMillis(10),
        retryBackoffMultiplier = 1.0,
    )
}
