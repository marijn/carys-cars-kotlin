package com.carshare.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

data class SmartcarLocation(val latitude: Double, val longitude: Double)
data class SmartcarBattery(val percentRemaining: Double, val range: Double)
data class SmartcarOdometer(val distance: Double)
data class SmartcarSignalResponse<T>(val data: T, val unitSystem: String? = null)

@Component
@Profile("!local")
class SmartcarClient(
    private val restTemplate: RestTemplate,
    @Value("\${smartcar.base-url}") private val baseUrl: String,
    @Value("\${smartcar.client-id}") private val clientId: String,
    @Value("\${smartcar.client-secret}") private val clientSecret: String
) : ISmartcarClient {

    override fun getLocation(smartcarId: String): SmartcarLocation? = try {
        val response = restTemplate.exchange(
            "$baseUrl/vehicles/$smartcarId/location",
            HttpMethod.GET, HttpEntity<Unit>(defaultHeaders()),
            Map::class.java
        )
        val data = response.body?.get("data") as? Map<*, *>
        SmartcarLocation(
            latitude = (data?.get("latitude") as? Number)?.toDouble() ?: 0.0,
            longitude = (data?.get("longitude") as? Number)?.toDouble() ?: 0.0
        )
    } catch (e: Exception) { null }

    override fun getBatteryLevel(smartcarId: String): Int? = try {
        val response = restTemplate.exchange(
            "$baseUrl/vehicles/$smartcarId/battery",
            HttpMethod.GET, HttpEntity<Unit>(defaultHeaders()),
            Map::class.java
        )
        val data = response.body?.get("data") as? Map<*, *>
        ((data?.get("percentRemaining") as? Number)?.toDouble()?.times(100))?.toInt()
    } catch (e: Exception) { null }

    override fun getOdometer(smartcarId: String): Double? = try {
        val response = restTemplate.exchange(
            "$baseUrl/vehicles/$smartcarId/odometer",
            HttpMethod.GET, HttpEntity<Unit>(defaultHeaders()),
            Map::class.java
        )
        val data = response.body?.get("data") as? Map<*, *>
        (data?.get("distance") as? Number)?.toDouble()
    } catch (e: Exception) { null }

    override fun lockVehicle(smartcarId: String): Boolean = try {
        val response = restTemplate.exchange(
            "$baseUrl/vehicles/$smartcarId/security",
            HttpMethod.POST, HttpEntity(mapOf("action" to "LOCK"), defaultHeaders()),
            Map::class.java
        )
        response.statusCode.is2xxSuccessful
    } catch (e: Exception) { false }

    override fun unlockVehicle(smartcarId: String): Boolean = try {
        val response = restTemplate.exchange(
            "$baseUrl/vehicles/$smartcarId/security",
            HttpMethod.POST, HttpEntity(mapOf("action" to "UNLOCK"), defaultHeaders()),
            Map::class.java
        )
        response.statusCode.is2xxSuccessful
    } catch (e: Exception) { false }

    private fun defaultHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }
}
