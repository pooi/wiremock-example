package org.example.wiremock.http.properties

import java.time.Duration

data class WebClientProperties(
    var name: String = "webClient",
    var url: String = "",
    var maxConnections: Int? = null,
    var connectionTimeout: Int = 3000,
    var readTimeout: Long = 3000L,
    var writeTimeout: Long = 9000L,
    var maxIdleTime: Duration? = null,
    var maxRetry: Long = 3,
    var retryDelay: Duration = Duration.ofSeconds(1)
)
