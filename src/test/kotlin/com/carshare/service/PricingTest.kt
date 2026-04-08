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
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional

class PricingTest {

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
    private val paymentIntent: PaymentIntent = mock()

    // Fixed base time so duration calculations are deterministic across machines and CI
    private val baseTime = Instant.parse("2024-01-01T10:00:00Z")

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
            status = VehicleStatus.RENTED,
            city = "Amsterdam",
            latitude = 52.3676,
            longitude = 4.9041
        )
        whenever(paymentIntent.id).thenReturn("pi_test")
        whenever(customerService.getCustomer(customer.id)).thenReturn(customer)
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(rentalRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(invoiceRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(stripeClient.chargeCustomer(any(), any(), any(), any())).thenReturn(paymentIntent)
        whenever(smartcarClient.getLocation("sc_test")).thenReturn(SmartcarLocation(52.3676, 4.9041))
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(12345.0)
        whenever(smartcarClient.lockVehicle("sc_test")).thenReturn(true)
    }

    /**
     * Builds a rental with a fixed startedAt and a pre-set endedAt so duration is
     * deterministic — not subject to clock drift between construction and assertion.
     */
    private fun rentalOf(durationMinutes: Long, startOdometer: Double = 12345.0): Rental {
        val startedAt = baseTime
        val endedAt   = baseTime.plusSeconds(durationMinutes * 60)
        return Rental(
            customer = customer,
            vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startOdometerKm = startOdometer,
            startedAt = startedAt,
            endedAt = endedAt          // pre-set so issueRentalInvoice uses this exact value
        )
    }

    @Test
    fun `13 minute rental costs 507 cents`() {
        val rental = rentalOf(durationMinutes = 13)
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(13, invoice.rentalMinutes)
        assertEquals(507, invoice.rentalCostCents)
        assertEquals(0, invoice.distanceOverageCostCents)
        assertEquals(507, invoice.totalCents)
    }

    @Test
    fun `minimum rental duration is 1 minute even for sub-minute trips`() {
        // endedAt is only 10 seconds after startedAt — rounds up to 1 minute
        val startedAt = baseTime
        val rental = Rental(
            customer = customer,
            vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startOdometerKm = 12345.0,
            startedAt = startedAt,
            endedAt = startedAt.plusSeconds(10)
        )
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(1, invoice.rentalMinutes)
        assertEquals(39, invoice.rentalCostCents)
    }

    @Test
    fun `trip under 200km has no distance overage`() {
        // 100km driven — within the free 200km limit
        val rental = rentalOf(durationMinutes = 60, startOdometer = 12000.0)
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(12100.0)

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(0, invoice.distanceOverageCostCents)
    }

    @Test
    fun `trip over 200km charges 30 cents per extra km`() {
        // 577km driven: 377km over the 200km limit
        // Distance overage: 377 * 30 = 11,310 cents
        // Rental cost:       60 min * 39 = 2,340 cents
        // Total:             13,650 cents
        val rental = rentalOf(durationMinutes = 60, startOdometer = 10000.0)
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))
        whenever(smartcarClient.getOdometer("sc_test")).thenReturn(10577.0)

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(577.0, invoice.distanceKm)
        assertEquals(11310, invoice.distanceOverageCostCents)
        assertEquals(2340, invoice.rentalCostCents)
        assertEquals(13650, invoice.totalCents)
    }

    @Test
    fun `invoice is charged to stripe and marked PAID`() {
        val rental = rentalOf(durationMinutes = 13)
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(InvoiceStatus.PAID, invoice.status)
        assertEquals("pi_test", invoice.stripePaymentIntentId)
        verify(stripeClient).chargeCustomer(
            stripeCustomerId = "cus_test",
            paymentMethodId = "pm_test",
            amountCents = invoice.totalCents,
            description = "CarShare invoice ${invoice.id}"
        )
    }

    @Test
    fun `invoice is marked FAILED when stripe throws`() {
        val rental = rentalOf(durationMinutes = 13)
        whenever(rentalRepository.findById(rental.id)).thenReturn(Optional.of(rental))
        whenever(stripeClient.chargeCustomer(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Card declined"))

        val invoice = service.endRental(rental.id, customer.id)

        assertEquals(InvoiceStatus.FAILED, invoice.status)
    }
}
