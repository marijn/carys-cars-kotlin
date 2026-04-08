package com.carshare.external

import com.stripe.model.PaymentIntent

interface ISmartcarClient {
    fun getLocation(smartcarId: String): SmartcarLocation?
    fun getBatteryLevel(smartcarId: String): Int?
    fun getOdometer(smartcarId: String): Double?
    fun lockVehicle(smartcarId: String): Boolean
    fun unlockVehicle(smartcarId: String): Boolean
}

interface IStripeClient {
    fun createCustomer(email: String, name: String): String
    fun chargeCustomer(
        stripeCustomerId: String,
        paymentMethodId: String,
        amountCents: Int,
        description: String
    ): PaymentIntent
}

interface IVeriffClient {
    fun createSession(customerId: java.util.UUID, fullName: String, email: String): VeriffSession?
    fun getSessionStatus(sessionId: String): String?
}
