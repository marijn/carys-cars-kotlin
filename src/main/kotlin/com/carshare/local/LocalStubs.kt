package com.carshare.local

import com.carshare.external.*
import com.stripe.model.PaymentIntent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

private val log = LoggerFactory.getLogger("LocalStubs")

@Component
@Profile("local")
class StubSmartcarClient : ISmartcarClient {
    // All vehicles are parked in the centre of Amsterdam
    override fun getLocation(smartcarId: String) =
        SmartcarLocation(latitude = 52.3676, longitude = 4.9041).also {
            log.info("[STUB] Smartcar getLocation($smartcarId) → Amsterdam centre")
        }

    override fun getBatteryLevel(smartcarId: String) = 75.also {
        log.info("[STUB] Smartcar getBatteryLevel($smartcarId) → 75%")
    }

    override fun getOdometer(smartcarId: String) = 12345.0.also {
        log.info("[STUB] Smartcar getOdometer($smartcarId) → 12345 km")
    }

    override fun lockVehicle(smartcarId: String) = true.also {
        log.info("[STUB] Smartcar lockVehicle($smartcarId) → ok")
    }

    override fun unlockVehicle(smartcarId: String) = true.also {
        log.info("[STUB] Smartcar unlockVehicle($smartcarId) → ok")
    }
}

@Component
@Profile("local")
class StubStripeClient : IStripeClient {
    override fun createCustomer(email: String, name: String): String {
        val id = "cus_local_${UUID.randomUUID().toString().take(8)}"
        log.info("[STUB] Stripe createCustomer($email) → $id")
        return id
    }

    override fun chargeCustomer(
        stripeCustomerId: String,
        paymentMethodId: String,
        amountCents: Int,
        description: String
    ): PaymentIntent {
        log.info("[STUB] Stripe charge $stripeCustomerId €${amountCents / 100.0} — $description → ok")
        // Return a minimally constructed PaymentIntent-like object
        // We only use .id from it, so set that via reflection
        val pi = PaymentIntent()
        val idField = pi.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(pi, "pi_local_${UUID.randomUUID().toString().take(8)}")
        return pi
    }
}

@Component
@Profile("local")
class StubVeriffClient : IVeriffClient {
    override fun createSession(customerId: UUID, fullName: String, email: String): VeriffSession {
        val sessionId = "sess_local_${UUID.randomUUID().toString().take(8)}"
        log.info("[STUB] Veriff createSession($fullName) → $sessionId")
        return VeriffSession(
            id = sessionId,
            url = "http://localhost:8080/api/kyc/local-approve?customerId=$customerId",
            status = "created"
        )
    }

    override fun getSessionStatus(sessionId: String) = "approved"
}
