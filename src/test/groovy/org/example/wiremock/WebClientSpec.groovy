package org.example.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.MatchResult
import org.example.wiremock.http.TestClient
import org.example.wiremock.utils.CoroutineTestUtils
import org.hamcrest.Matchers
import org.junit.ClassRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClientResponseException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WebClientSpec extends Specification {

    @Shared
    ObjectMapper objectMapper = new ObjectMapper()

    @ClassRule
    @Shared
    WireMockRule server = new WireMockRule(10001)

    @Autowired
    TestClient testClient

    def setupSpec() {
        server.start()
    }

    def cleanup() {
        assert !server.findUnmatchedRequests().requests
        server.resetAll()
    }

    def cleanupSpec() {
        server.stop()
    }

    def "get"() {
        given:
        def expectedResult = [
            test: "123"
        ]
        server.addStubMapping(
            get(urlPathEqualTo("/main"))
                .willReturn(okJson(objectMapper.writeValueAsString(expectedResult)))
                .build()
        )

        when:
        def result = CoroutineTestUtils.executeSuspendFun {
            testClient.callWithResponse(Map, HttpMethod.GET, "/main", null, null, it)
        } as Map<String, String>

        then:
        result == expectedResult
        server.verify(1, getRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def "post"() {
        given:
        server.addStubMapping(
            post(urlPathEqualTo("/main"))
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithoutResponse(HttpMethod.POST, "/main", null, null, it)
        }

        then:
        server.verify(1, postRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def "patch"() {
        given:
        server.addStubMapping(
            patch(urlPathEqualTo("/main"))
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithoutResponse(HttpMethod.PATCH, "/main", null, null, it)
        }

        then:
        server.verify(1, patchRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def "delete"() {
        given:
        server.addStubMapping(
            delete(urlPathEqualTo("/main"))
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithoutResponse(HttpMethod.DELETE, "/main", null, null, it)
        }

        then:
        server.verify(1, deleteRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def "get with query params - 1"() {
        given:
        def expectedResult = [
            test: "123"
        ]
        def queryParams = [
            "test": ["test1"]
        ]
        server.addStubMapping(
            get(urlEqualTo("/main?test=test1"))
                .willReturn(okJson(objectMapper.writeValueAsString(expectedResult)))
                .build()
        )

        when:
        def result = CoroutineTestUtils.executeSuspendFun {
            testClient.callWithResponse(Map, HttpMethod.GET, "/main", queryParams, null, it)
        } as Map<String, String>

        then:
        result == expectedResult
        server.verify(1, getRequestedFor(urlEqualTo("/main?test=test1")))
        0 * _
    }

    def "get with query params - 2"() {
        given:
        def expectedResult = [
            test: "123"
        ]
        def queryParams = [
            "test": ["test1"]
        ]
        server.addStubMapping(
            get(urlPathEqualTo("/main"))
                .withQueryParam("test", equalTo("test1"))
                .willReturn(okJson(objectMapper.writeValueAsString(expectedResult)))
                .build()
        )

        when:
        def result = CoroutineTestUtils.executeSuspendFun {
            testClient.callWithResponse(Map, HttpMethod.GET, "/main", queryParams, null, it)
        } as Map<String, String>

        then:
        result == expectedResult
        server.verify(
            1,
            getRequestedFor(urlPathEqualTo("/main"))
                .withQueryParam("test", equalTo("test1"))
        )
        0 * _
    }

    def "get with query params - 3"() {
        given:
        def expectedResult = [
            test: "123"
        ]
        def queryParams = [
            "test": ["test1", "test2", "test3"]
        ]
        def mappingBuilder = get(urlPathEqualTo("/main"))
            .willReturn(okJson(objectMapper.writeValueAsString(expectedResult)))

        queryParams.entrySet().each {
            mappingBuilder.andMatching { request ->
                MatchResult.of(
                    Matchers.containsInAnyOrder(it.value as String[])
                        .matches(request.queryParameter(it.getKey()).values())
                )
            }
        }

        server.addStubMapping(mappingBuilder.build())

        when:
        def result = CoroutineTestUtils.executeSuspendFun {
            testClient.callWithResponse(Map, HttpMethod.GET, "/main", queryParams, null, it)
        } as Map<String, String>

        then:
        result == expectedResult
        server.verify(1, getRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def "get with headers"() {
        given:
        def expectedResult = [
            test: "123"
        ]
        def headers = [
            "test": ["test1"]
        ]
        server.addStubMapping(
            get(urlPathEqualTo("/main"))
                .withHeader("test", equalTo("test1"))
                .willReturn(okJson(objectMapper.writeValueAsString(expectedResult)))
                .build()
        )

        when:
        def result = CoroutineTestUtils.executeSuspendFun {
            testClient.callWithResponse(Map, HttpMethod.GET, "/main", null, headers, it)
        } as Map<String, String>

        then:
        result == expectedResult
        server.verify(
            1,
            getRequestedFor(urlPathEqualTo("/main"))
                .withHeader("test", equalTo("test1"))
        )
        0 * _
    }

    def "post with body"() {
        given:
        def body = [
            "example1": "test1",
            "example2": "test2"
        ]
        server.addStubMapping(
            post(urlPathEqualTo("/main"))
                .withRequestBody(equalToJson(toJson(
                    [
                        "example1": "test1"
                    ]
                ), true, true))
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithBody(HttpMethod.POST, "/main", null, null, body, it)
        }

        then:
        server.verify(1, postRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    @Unroll
    def "error - #status"() {
        given:
        server.addStubMapping(
            post(urlPathEqualTo("/main"))
                .willReturn(response)
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithoutResponse(HttpMethod.POST, "/main", null, null, it)
        }

        then:
        def exception = thrown(WebClientResponseException)
        exception.rawStatusCode == status
        server.verify(1, postRequestedFor(urlPathEqualTo("/main")))
        0 * _

        where:
        response       || status
        badRequest()   || 400
        unauthorized() || 401
        forbidden()    || 403
        notFound()     || 404
        serverError()  || 500
        status(422)    || 422
    }

    def "error with body"() {
        given:
        def errorBody = [
            code   : 404,
            message: "Device not found"
        ]
        server.addStubMapping(
            post(urlPathEqualTo("/main"))
                .willReturn(badRequest().withBody(toJson(errorBody)))
                .build()
        )

        when:
        CoroutineTestUtils.executeSuspendFun {
            testClient.callWithoutResponse(HttpMethod.POST, "/main", null, null, it)
        }

        then:
        def exception = thrown(WebClientResponseException)
        exception.rawStatusCode == 400
        exception.responseBodyAsString == toJson(errorBody)
        server.verify(1, postRequestedFor(urlPathEqualTo("/main")))
        0 * _
    }

    def toJson(value) {
        objectMapper.writeValueAsString(value)
    }
}
