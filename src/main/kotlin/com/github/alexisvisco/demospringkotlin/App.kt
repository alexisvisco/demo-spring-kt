package com.github.alexisvisco.demospringkotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoSpringKotlinApplication

fun main(args: Array<String>) {
    runApplication<DemoSpringKotlinApplication>(*args)
}
