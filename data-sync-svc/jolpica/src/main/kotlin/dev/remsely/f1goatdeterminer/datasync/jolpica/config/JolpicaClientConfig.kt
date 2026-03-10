package dev.remsely.f1goatdeterminer.datasync.jolpica.config

import dev.remsely.f1goatdeterminer.datasync.jolpica.api.JolpicaApi
import dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor.RateLimitInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

private val log = KotlinLogging.logger {}

@Configuration
class JolpicaClientConfig {

    @Bean
    fun jolpicaRestClient(
        properties: JolpicaClientProperties,
        rateLimitInterceptor: RateLimitInterceptor,
    ): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        }
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor(rateLimitInterceptor)
            .build()
    }

    @Bean
    fun jolpicaApi(jolpicaRestClient: RestClient): JolpicaApi {
        val adapter = RestClientAdapter.create(jolpicaRestClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient<JolpicaApi>()
    }

    @Bean
    fun jolpicaRetry(properties: JolpicaClientProperties): Retry {
        val intervalFunction = IntervalFunction.ofExponentialBackoff(
            properties.retryWaitDuration,
            properties.retryBackoffMultiplier,
        )

        val config = RetryConfig.custom<Any>()
            .maxAttempts(properties.retryMaxAttempts)
            .intervalFunction(intervalFunction)
            .retryOnException { it is org.springframework.web.client.RestClientException }
            .build()

        val retry = Retry.of("jolpica", config)

        retry.eventPublisher.onRetry { event ->
            val waitMs = intervalFunction.apply(event.numberOfRetryAttempts)
            log.warn {
                "~~ Jolpica API retry: attempt ${event.numberOfRetryAttempts}/${properties.retryMaxAttempts}, " +
                    "waiting ${waitMs}ms " +
                    "(${event.lastThrowable.javaClass.simpleName}: ${event.lastThrowable.message?.take(100)})"
            }
        }

        return retry
    }
}
