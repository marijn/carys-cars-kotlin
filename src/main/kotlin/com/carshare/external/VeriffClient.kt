package com.carshare.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

data class VeriffSession(val id: String, val url: String, val status: String)

@Component
@Profile("!local")
class VeriffClient(
    private val restTemplate: RestTemplate,
    @Value("\${veriff.base-url}") private val baseUrl: String,
    @Value("\${veriff.api-key}") private val apiKey: String
) : IVeriffClient {

    override fun createSession(customerId: UUID, fullName: String, email: String): VeriffSession? = try {
        val body = mapOf(
            "verification" to mapOf(
                "callback" to "https://your-domain.com/api/kyc/callback",
                "person" to mapOf("fullName" to fullName),
                "vendorData" to customerId.toString(),
                "document" to mapOf("type" to "DRIVERS_LICENSE")
            )
        )
        val response = restTemplate.exchange(
            "$baseUrl/sessions",
            HttpMethod.POST,
            HttpEntity(body, defaultHeaders()),
            Map::class.java
        )
        val verification = (response.body?.get("verification") as? Map<*, *>)
        VeriffSession(
            id = verification?.get("id")?.toString() ?: "",
            url = verification?.get("url")?.toString() ?: "",
            status = verification?.get("status")?.toString() ?: ""
        )
    } catch (e: Exception) { null }

    override fun getSessionStatus(sessionId: String): String? = try {
        val response = restTemplate.exchange(
            "$baseUrl/sessions/$sessionId",
            HttpMethod.GET,
            HttpEntity<Unit>(defaultHeaders()),
            Map::class.java
        )
        val verification = (response.body?.get("verification") as? Map<*, *>)
        verification?.get("status")?.toString()
    } catch (e: Exception) { null }

    private fun defaultHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("X-AUTH-CLIENT", apiKey)
        return headers
    }
}
