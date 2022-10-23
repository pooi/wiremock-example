package org.example.wiremock.http

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.example.wiremock.http.properties.WebClientProperties
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.util.concurrent.TimeUnit

@Component
class WebClientFactory {

    fun createWebClient(
        properties: WebClientProperties,
        block: WebClient.Builder.() -> Unit = {}
    ): WebClient = WebClient.builder()
        .clientConnector(properties.toConnector())
        .baseUrl(properties.url)
        .apply(block)
        .build()

    private fun WebClientProperties.toConnector(): ReactorClientHttpConnector {
        val sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()

        val provider = ConnectionProvider.builder("$name-provider")
            .apply {
                if (maxConnections != null) {
                    maxConnections(maxConnections!!)
                }
                if (maxIdleTime != null) {
                    maxIdleTime(maxIdleTime!!)
                }
            }
            .build()

        val httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .secure { it.sslContext(sslContext) }
            .doOnConnected {
                it
                    .addHandlerLast(ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
            }
        return ReactorClientHttpConnector(httpClient)
    }
}
