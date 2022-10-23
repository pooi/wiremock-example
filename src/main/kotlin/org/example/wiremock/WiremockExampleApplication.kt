package org.example.wiremock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WiremockExampleApplication

fun main(args: Array<String>) {
    runApplication<WiremockExampleApplication>(*args)
}
