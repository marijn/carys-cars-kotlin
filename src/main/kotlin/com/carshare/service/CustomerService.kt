package com.carshare.service

import com.carshare.domain.Customer
import com.carshare.domain.KycStatus
import com.carshare.external.IStripeClient
import com.carshare.external.IVeriffClient
import com.carshare.external.VeriffSession
import com.carshare.repository.CustomerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val stripeClient: IStripeClient,
    private val veriffClient: IVeriffClient
) {
    @Transactional
    fun getOrCreateCustomer(oauthSubject: String, email: String, fullName: String): Customer {
        return customerRepository.findByOauthSubject(oauthSubject) ?: run {
            val stripeId = stripeClient.createCustomer(email, fullName)
            customerRepository.save(Customer(
                oauthSubject = oauthSubject,
                email = email,
                fullName = fullName,
                stripeCustomerId = stripeId
            ))
        }
    }

    fun getCustomer(id: UUID): Customer =
        customerRepository.findById(id).orElseThrow { IllegalArgumentException("Customer not found") }

    @Transactional
    fun initiateKyc(customerId: UUID): String {
        val customer = getCustomer(customerId)
        val session = veriffClient.createSession(customerId, customer.fullName, customer.email)
            ?: throw IllegalStateException("Failed to create Veriff session")
        customer.veriffSessionId = session.id
        customer.updatedAt = Instant.now()
        customerRepository.save(customer)
        return session.url
    }

    @Transactional
    fun handleKycCallback(sessionId: String, status: String) {
        val customer = customerRepository.findAll().firstOrNull { it.veriffSessionId == sessionId }
            ?: return
        customer.kycStatus = when (status) {
            "approved" -> KycStatus.APPROVED
            "declined", "abandoned" -> KycStatus.REJECTED
            else -> KycStatus.PENDING
        }
        customer.updatedAt = Instant.now()
        customerRepository.save(customer)
    }

    @Transactional
    fun addPaymentMethod(customerId: UUID, paymentMethodId: String) {
        val customer = getCustomer(customerId)
        customer.stripePaymentMethodId = paymentMethodId
        customer.updatedAt = Instant.now()
        customerRepository.save(customer)
    }

    fun isReadyToRent(customer: Customer): Boolean {
        return customer.kycStatus == KycStatus.APPROVED && customer.stripePaymentMethodId != null
    }
}
