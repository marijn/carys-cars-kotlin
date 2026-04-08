package com.carshare.service

import com.carshare.domain.Customer
import com.carshare.domain.KycStatus
import com.carshare.external.IStripeClient
import com.carshare.external.IVeriffClient
import com.carshare.external.VeriffSession
import com.carshare.repository.CustomerRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import java.util.UUID

class CustomerServiceTest {

    private val customerRepository: CustomerRepository = mock()
    private val stripeClient: IStripeClient = mock()
    private val veriffClient: IVeriffClient = mock()

    private val service = CustomerService(
        customerRepository = customerRepository,
        stripeClient = stripeClient,
        veriffClient = veriffClient
    )

    @Test
    fun `getOrCreateCustomer creates new customer when not found`() {
        whenever(customerRepository.findByOauthSubject("oauth|new")).thenReturn(null)
        whenever(stripeClient.createCustomer(any(), any())).thenReturn("cus_new")
        whenever(customerRepository.save(any())).thenAnswer { it.arguments[0] }

        val customer = service.getOrCreateCustomer("oauth|new", "new@example.com", "New User")

        assertEquals("oauth|new", customer.oauthSubject)
        assertEquals("new@example.com", customer.email)
        assertEquals("cus_new", customer.stripeCustomerId)
        verify(stripeClient).createCustomer("new@example.com", "New User")
    }

    @Test
    fun `getOrCreateCustomer returns existing customer`() {
        val existing = Customer(
            oauthSubject = "oauth|existing",
            email = "e@example.com",
            fullName = "Existing User",
            stripeCustomerId = "cus_existing"
        )
        whenever(customerRepository.findByOauthSubject("oauth|existing")).thenReturn(existing)

        val result = service.getOrCreateCustomer("oauth|existing", "e@example.com", "Existing User")

        assertEquals(existing.id, result.id)
        verify(stripeClient, never()).createCustomer(any(), any())
    }

    @Test
    fun `isReadyToRent returns true when KYC approved and payment method set`() {
        val customer = Customer(
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test",
            kycStatus = KycStatus.APPROVED,
            stripePaymentMethodId = "pm_123"
        )
        assertTrue(service.isReadyToRent(customer))
    }

    @Test
    fun `isReadyToRent returns false when KYC pending`() {
        val customer = Customer(
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test",
            kycStatus = KycStatus.PENDING,
            stripePaymentMethodId = "pm_123"
        )
        assertFalse(service.isReadyToRent(customer))
    }

    @Test
    fun `isReadyToRent returns false when no payment method`() {
        val customer = Customer(
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test",
            kycStatus = KycStatus.APPROVED,
            stripePaymentMethodId = null
        )
        assertFalse(service.isReadyToRent(customer))
    }

    @Test
    fun `handleKycCallback sets status to APPROVED`() {
        val customer = Customer(
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test",
            kycStatus = KycStatus.PENDING,
            veriffSessionId = "session_abc"
        )
        whenever(customerRepository.findAll()).thenReturn(listOf(customer))
        whenever(customerRepository.save(any())).thenAnswer { it.arguments[0] }

        service.handleKycCallback("session_abc", "approved")

        verify(customerRepository).save(argThat { kycStatus == KycStatus.APPROVED })
    }

    @Test
    fun `handleKycCallback sets status to REJECTED on declined`() {
        val customer = Customer(
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test",
            kycStatus = KycStatus.PENDING,
            veriffSessionId = "session_xyz"
        )
        whenever(customerRepository.findAll()).thenReturn(listOf(customer))
        whenever(customerRepository.save(any())).thenAnswer { it.arguments[0] }

        service.handleKycCallback("session_xyz", "declined")

        verify(customerRepository).save(argThat { kycStatus == KycStatus.REJECTED })
    }

    @Test
    fun `initiateKyc saves veriff session id and returns url`() {
        val customerId = UUID.randomUUID()
        val customer = Customer(
            id = customerId,
            oauthSubject = "o",
            email = "e@x.com",
            fullName = "Test User"
        )
        whenever(customerRepository.findById(customerId)).thenReturn(Optional.of(customer))
        whenever(veriffClient.createSession(customerId, "Test User", "e@x.com"))
            .thenReturn(VeriffSession(id = "sess_1", url = "https://veriff.me/sess_1", status = "created"))
        whenever(customerRepository.save(any())).thenAnswer { it.arguments[0] }

        val url = service.initiateKyc(customerId)

        assertEquals("https://veriff.me/sess_1", url)
        verify(customerRepository).save(argThat { veriffSessionId == "sess_1" })
    }

    @Test
    fun `addPaymentMethod stores payment method on customer`() {
        val customerId = UUID.randomUUID()
        val customer = Customer(id = customerId, oauthSubject = "o", email = "e@x.com", fullName = "T")
        whenever(customerRepository.findById(customerId)).thenReturn(Optional.of(customer))
        whenever(customerRepository.save(any())).thenAnswer { it.arguments[0] }

        service.addPaymentMethod(customerId, "pm_abc123")

        verify(customerRepository).save(argThat { stripePaymentMethodId == "pm_abc123" })
    }
}
