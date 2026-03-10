package dev.remsely.f1goatdeterminer.datasync.jolpica.config

import com.sun.net.httpserver.HttpServer
import dev.remsely.f1goatdeterminer.datasync.jolpica.api.JolpicaApi
import dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor.RateLimitInterceptor
import io.github.resilience4j.retry.Retry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

class JolpicaClientConfigTest {

    @Test
    fun `context creates Jolpica beans and api proxy uses configured base url`() {
        contextRunner().run { context ->
            val restClient = context.getBean<RestClient>()
            val retry = context.getBean<Retry>()
            val api = context.getBean<JolpicaApi>()

            restClient.shouldNotBeNull()
            retry.shouldNotBeNull()

            val response = api.fetchStatuses(limit = 2, offset = 7)

            response.mrData.totalInt shouldBe 1
            lastQuery.get() shouldBe "limit=2&offset=7"
        }
    }

    @Test
    fun `retry bean honors configured max attempts`() {
        contextRunner(maxAttempts = 3).run { context ->
            val retry = context.getBean<Retry>()
            var attempts = 0

            val supplier = Retry.decorateSupplier(retry) {
                attempts++
                throw RestClientException("boom")
            }

            shouldThrow<RestClientException> { supplier.get() }
            attempts shouldBe 3
        }
    }

    private fun contextRunner(maxAttempts: Int = 2) = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java, JolpicaClientConfig::class.java)
        .withPropertyValues(
            "integration.jolpica.base-url=http://localhost:${server.address.port}",
            "integration.jolpica.connect-timeout=1s",
            "integration.jolpica.read-timeout=1s",
            "integration.jolpica.page-size=25",
            "integration.jolpica.rate-limit-rps=1000.0",
            "integration.jolpica.retry-max-attempts=$maxAttempts",
            "integration.jolpica.retry-wait-duration=10ms",
            "integration.jolpica.retry-backoff-multiplier=1.0",
        )

    companion object {
        private lateinit var server: HttpServer
        private val lastQuery = AtomicReference<String>()

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server = HttpServer.create(InetSocketAddress(0), 0)
            server.createContext("/status.json") { exchange ->
                lastQuery.set(exchange.requestURI.query)
                val body =
                    """
                    {
                      "MRData": {
                        "limit": "2",
                        "offset": "7",
                        "total": "1",
                        "StatusTable": {
                          "Status": [
                            {"statusId": "1", "count": "100", "status": "Finished"}
                          ]
                        }
                      }
                    }
                    """.trimIndent().toByteArray(StandardCharsets.UTF_8)

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }
    }

    @Configuration
    @EnableConfigurationProperties(JolpicaClientProperties::class)
    class TestConfig {
        @Bean
        fun rateLimitInterceptor(properties: JolpicaClientProperties): RateLimitInterceptor =
            RateLimitInterceptor(properties)
    }
}
