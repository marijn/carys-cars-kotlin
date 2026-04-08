package com.carshare.service

import com.carshare.domain.*
import com.carshare.external.ISmartcarClient
import com.carshare.external.SmartcarLocation
import com.carshare.external.IStripeClient
import com.carshare.repository.*
import com.stripe.model.PaymentIntent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional

class RentalServiceTest {

    private val rentalRepository: RentalRepository = mock()
    private val vehicleRepository: VehicleRepository = mock()
    private val invoiceRepository: InvoiceRepository = mock()
    private val reservationService: ReservationService = mock()
    private val customerService: CustomerService = mock()
    private val smartcarClient: ISmartcarClient = mock()
    private val stripeClient: IStripeClient = mock()

    private val service = RentalService(
        rentalRepository = rentalRepository,
        vehicleRepository = vehicleRepository,
        invoiceRepository = invoiceRepository,
        reservationService = reservationService,
        customerService = customerService,
        smartcarClient = smartcarClient,
        stripeClient = stripeClient,
        ratePerMinute = 39,
        maxKmFree = 200,
        overageRatePerKm = 30
    )

    private lateinit var customer: Customer
    private lateinit var vehicle: Vehicle
    private lateinit var reservation: Reservation

    @BeforeEach
    fun setUp() {
        customer = Customer(
            oauthSubject = "oauth|test",
            email = "t@example.com",
            fullName = "Test Driver",
            stripeCustomerId = "cus_test",
            stripePaymentMethodId = "pm_test",
            kycStatus = KycStatus.APPROVED
        )
        vehicle = Vehicle(
            smartcarId = "sc_test",
            licensePlate = "XX-111-YY",
            vehicleClass = VehicleClass.CITY_SMALL,
            status = VehicleStatus.RESERVED,
            city = "Amsterdam",
            latitude = 52.3676,
            longitude = 4.9041
        )
        reservation = Reservation(
            customer = customer,
            vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plusSeconds(600)
        )
    }

    @Test
    fun `startRental reads odometer, unlocks vehicle and creates rental`() {
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(true)
        whenever(reservationService.convertToRental(reservation.id, customer.id)).thenReturn(reservation)
        whenever(reservationService.computeReservationCost(any(), any())).thenReturn(0)
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(12345.0)
        whenever(smartcarClient.unlockVehicle("sc_test")).thenReturn(true)
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(rentalRepository.save(any())).thenAnswer { it.arguments[0] }

        val rental = service.startRental(customer.id, reservation.id)

        assertEquals(RentalStatus.ACTIVE, rental.status)
        assertEquals(customer.id, rental.customer.id)
        assertEquals(12345.0, rental.startOdometerKm)
        verify(smartcarClient).unlockVehicle("sc_test")
        verify(vehicleRepository).save(argThat { status == VehicleStatus.RENTED })
    }

    @Test
    fun `startRental throws when customer not ready`() {
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(customerService.isReadyToRent(customer)).thenReturn(false)

        assertThrows<IllegalStateException> {
            service.startRental(customer.id, reservation.id)
        }
    }

    @Test
    fun `endRental records end odometer, locks vehicle and issues invoice`() {
        val rental = Rental(
            customer = customer,
            vehicle = vehicle,
            reservation = reservation,
            status = RentalStatus.ACTIVE,
            startOdometerKm = 12000.0,
            startedAt = Instant.now().minusSeconds(600)
        )
        val paymentIntent: PaymentIntent = mock()
        whenever(paymentIntent.id).thenReturn("pi_test")

        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))
        whenever(smartcarClient.getLocation("sc_test")).thenReturn(SmartcarLocation(52.3676, 4.9041))
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(12050.0)
        whenever(smartcarClient.lockVehicle("sc_test")).thenReturn(true)
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(rentalRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(invoiceRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(stripeClient.chargeCustomer(any(), any(), any(), any())).thenReturn(paymentIntent)

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(InvoiceType.RENTAL, invoice.type)
        assertTrue(invoice.rentalCostCents > 0)
        verify(smartcarClient).lockVehicle("sc_test")
        verify(vehicleRepository).save(argThat { status == VehicleStatus.AVAILABLE })
        verify(rentalRepository).save(argThat { endOdometerKm == 12050.0 })
    }

    @Test
    fun `endRental throws when vehicle is outside operating region`() {
        val rental = Rental(
            customer = customer,
            vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startOdometerKm = 12000.0,
            startedAt = Instant.now().minusSeconds(300)
        )
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))
        whenever(smartcarClient.getLocation("sc_test")).thenReturn(SmartcarLocation(0.0, 0.0))
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(12050.0)

        assertThrows<IllegalStateException> {
            service.endRental(rental.id, customer.id)
        }
    }

    @Test
    fun `endRental throws when rental does not belong to customer`() {
        val otherCustomer = Customer(oauthSubject = "other", email = "o@x.com", fullName = "Other")
        val rental = Rental(
            customer = otherCustomer,
            vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startedAt = Instant.now().minusSeconds(300)
        )
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))

        assertThrows<IllegalStateException> {
            service.endRental(rental.id, customer.id)
        }
    }

    @Test
    fun `getActiveRental returns null when no active rental`() {
        whenever(rentalRepository.findByCustomerIdAndStatus(customer.id, RentalStatus.ACTIVE))
            .thenReturn(emptyList())

        assertNull(service.getActiveRental(customer.id))
    }
}
