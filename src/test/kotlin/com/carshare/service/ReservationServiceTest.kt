package com.carshare.service

import com.carshare.domain.*
import com.carshare.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ReservationServiceTest {

    private val reservationRepository: ReservationRepository = mock()
    private val vehicleRepository: VehicleRepository = mock()
    private val customerService: CustomerService = mock()
    private val dailyUsageRepository: CustomerDailyReservationUsageRepository = mock()

    private val service = ReservationService(
        reservationRepository = reservationRepository,
        vehicleRepository = vehicleRepository,
        customerService = customerService,
        dailyUsageRepository = dailyUsageRepository,
        freeMinutesPerDay = 20,
        expiryMinutes = 20L,
        overageRateCents = 20
    )

    private lateinit var customer: Customer
    private lateinit var vehicle: Vehicle

    @BeforeEach
    fun setUp() {
        customer = Customer(
            oauthSubject = "oauth|123",
            email = "test@example.com",
            fullName = "Test User",
            stripeCustomerId = "cus_123",
            stripePaymentMethodId = "pm_123",
            kycStatus = KycStatus.APPROVED
        )
        vehicle = Vehicle(
            smartcarId = "sc_abc",
            licensePlate = "AB-123-CD",
            vehicleClass = VehicleClass.CITY_SMALL,
            status = VehicleStatus.AVAILABLE,
            city = "Amsterdam"
        )
    }

    @Test
    fun `createReservation succeeds for approved customer with available vehicle`() {
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(true)
        whenever(vehicleRepository.findById(vehicle.id)).thenReturn(Optional.of(vehicle))
        whenever(reservationRepository.findByCustomerIdAndStatus(customer.id, ReservationStatus.ACTIVE))
            .thenReturn(emptyList())
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(reservationRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.createReservation(customer.id, vehicle.id)

        assertEquals(ReservationStatus.ACTIVE, result.status)
        assertEquals(customer.id, result.customer.id)
        assertEquals(vehicle.id, result.vehicle.id)
        verify(vehicleRepository).save(argThat { status == VehicleStatus.RESERVED })
    }

    @Test
    fun `createReservation throws when customer is not ready to rent`() {
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(false)

        assertThrows<IllegalStateException> {
            service.createReservation(customer.id, vehicle.id)
        }
    }

    @Test
    fun `createReservation throws when vehicle is not available`() {
        val reservedVehicle = vehicle.copy(status = VehicleStatus.RESERVED)
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(true)
        whenever(vehicleRepository.findById(reservedVehicle.id)).thenReturn(Optional.of(reservedVehicle))

        assertThrows<IllegalStateException> {
            service.createReservation(customer.id, reservedVehicle.id)
        }
    }

    @Test
    fun `createReservation throws when customer already has active reservation`() {
        val existing = Reservation(customer = customer, vehicle = vehicle,
            expiresAt = Instant.now().plusSeconds(600))
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(true)
        whenever(vehicleRepository.findById(vehicle.id)).thenReturn(Optional.of(vehicle))
        whenever(reservationRepository.findByCustomerIdAndStatus(customer.id, ReservationStatus.ACTIVE))
            .thenReturn(listOf(existing))

        assertThrows<IllegalStateException> {
            service.createReservation(customer.id, vehicle.id)
        }
    }

    @Test
    fun `cancelReservation sets status to CANCELLED and frees the vehicle`() {
        val reservation = Reservation(
            customer = customer,
            vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plusSeconds(600)
        )
        whenever(reservationRepository.findById(reservation.id)).thenReturn(Optional.of(reservation))
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(reservationRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.cancelReservation(reservation.id, customer.id)

        assertEquals(ReservationStatus.CANCELLED, result.status)
        assertNotNull(result.endedAt)
        verify(vehicleRepository).save(argThat { status == VehicleStatus.AVAILABLE })
    }

    @Test
    fun `cancelReservation throws when reservation belongs to different customer`() {
        val otherCustomer = Customer(oauthSubject = "other", email = "other@x.com", fullName = "Other")
        val reservation = Reservation(
            customer = otherCustomer,
            vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plusSeconds(600)
        )
        whenever(reservationRepository.findById(reservation.id)).thenReturn(Optional.of(reservation))

        assertThrows<IllegalStateException> {
            service.cancelReservation(reservation.id, customer.id)
        }
    }

    @Test
    fun `extendReservation moves expiry forward`() {
        val originalExpiry = Instant.now().plusSeconds(300)
        val reservation = Reservation(
            customer = customer,
            vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = originalExpiry
        )
        whenever(reservationRepository.findById(reservation.id)).thenReturn(Optional.of(reservation))
        whenever(reservationRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.extendReservation(reservation.id, customer.id, 10L)

        assertTrue(result.expiresAt.isAfter(originalExpiry))
    }

    @Test
    fun `computeReservationCost is free within daily allowance`() {
        val reservation = Reservation(
            customer = customer,
            vehicle = vehicle,
            reservedAt = Instant.now().minusSeconds(600), // 10 minutes ago
            expiresAt = Instant.now().plusSeconds(600)
        )
        val usage = CustomerDailyReservationUsage(
            customer = customer,
            usageDate = LocalDate.now(),
            freeMinutesUsed = 0
        )
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(dailyUsageRepository.findByCustomerIdAndUsageDate(customer.id, LocalDate.now()))
            .thenReturn(usage)

        val cost = service.computeReservationCost(reservation, customer.id)

        assertEquals(0, cost)
    }

    @Test
    fun `computeReservationCost charges overage when daily allowance exhausted`() {
        val reservation = Reservation(
            customer = customer,
            vehicle = vehicle,
            reservedAt = Instant.now().minusSeconds(1800), // 30 minutes ago
            expiresAt = Instant.now().plusSeconds(60)
        )
        val usage = CustomerDailyReservationUsage(
            customer = customer,
            usageDate = LocalDate.now(),
            freeMinutesUsed = 20 // all free minutes used
        )
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(dailyUsageRepository.findByCustomerIdAndUsageDate(customer.id, LocalDate.now()))
            .thenReturn(usage)

        val cost = service.computeReservationCost(reservation, customer.id)

        // 30 minutes * 20 cents = 600 cents (all overage, no free left)
        assertEquals(600, cost)
    }
}
