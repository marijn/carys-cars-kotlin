package com.carshare.external

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.PaymentIntent
import com.stripe.param.CustomerCreateParams
import com.stripe.param.PaymentIntentCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

@Component
@Profile("!local")
class StripeClient(
    @Value("\${stripe.api-key}") private val apiKey: String
) : IStripeClient {

    @PostConstruct
    fun init() { Stripe.apiKey = apiKey }

    override fun createCustomer(email: String, name: String): String {
        val params = CustomerCreateParams.builder()
            .setEmail(email)
            .setName(name)
            .build()
        return Customer.create(params).id
    }

    override fun chargeCustomer(
        stripeCustomerId: String,
        paymentMethodId: String,
        amountCents: Int,
        description: String
    ): PaymentIntent {
        val params = PaymentIntentCreateParams.builder()
            .setAmount(amountCents.toLong())
            .setCurrency("eur")
            .setCustomer(stripeCustomerId)
            .setPaymentMethod(paymentMethodId)
            .setConfirm(true)
            .setOffSession(true)
            .setDescription(description)
            .build()
        return PaymentIntent.create(params)
    }
}
