package com.carshare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CarShareApplication

fun main(args: Array<String>) {
    runApplication<CarShareApplication>(*args)
}
