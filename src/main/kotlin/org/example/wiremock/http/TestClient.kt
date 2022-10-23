package org.example.wiremock.http

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.example.wiremock.http.properties.WebClientProperties
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class TestClient(
    webClientFactory: WebClientFactory
) {
    private val webClient = webClientFactory.createWebClient(
        WebClientProperties(
            name = "test",
            url = "http://localhost:10001",
            maxRetry = 0
        )
    )

    suspend fun <T> callWithResponse(
        clazz: Class<T>,
        method: HttpMethod,
        path: String,
        params: Map<String, List<String>>? = null,
        headers: Map<String, List<String>>? = null
    ): T? = call(method, path, params, headers)
        .bodyToMono(clazz)
        .awaitSingleOrNull()

    suspend fun <T> callWithoutResponse(
        method: HttpMethod,
        path: String,
        params: Map<String, List<String>>? = null,
        headers: Map<String, List<String>>? = null
    ) {
        call(method, path, params, headers)
            .bodyToMono(Void::class.java)
            .awaitSingleOrNull()
    }

    suspend fun <T> callWithBody(
        method: HttpMethod,
        path: String,
        params: Map<String, List<String>>? = null,
        headers: Map<String, List<String>>? = null,
        body: Any? = null
    ) {
        call(method, path, params, headers, body)
            .bodyToMono(Void::class.java)
            .awaitSingleOrNull()
    }

    private suspend fun call(
        method: HttpMethod,
        path: String,
        params: Map<String, List<String>>? = null,
        headers: Map<String, List<String>>? = null,
        body: Any? = null
    ): WebClient.ResponseSpec {
        val request = webClient
            .method(method)
            .uri {
                it.path(path)
                params?.entries?.forEach { entry ->
                    it.queryParam(entry.key, entry.value)
                }
                it.build()
            }
            .headers {
                headers?.entries?.forEach { entry ->
                    it[entry.key] = entry.value
                }
            }

        if (body != null) {
            request.bodyValue(body)
        }

        return request.retrieve()
    }
}
