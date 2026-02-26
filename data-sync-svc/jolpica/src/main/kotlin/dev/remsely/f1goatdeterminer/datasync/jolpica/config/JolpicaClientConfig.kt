package dev.remsely.f1goatdeterminer.datasync.jolpica.config

import dev.remsely.f1goatdeterminer.datasync.jolpica.api.JolpicaApi
import dev.remsely.f1goatdeterminer.datasync.jolpica.interceptor.RateLimitInterceptor
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

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
        val config = RetryConfig.custom<Any>()
            .maxAttempts(properties.retryMaxAttempts)
            .waitDuration(properties.retryWaitDuration)
            .retryOnException { it is org.springframework.web.client.RestClientException }
            .build()
        return Retry.of("jolpica", config)
    }
}
