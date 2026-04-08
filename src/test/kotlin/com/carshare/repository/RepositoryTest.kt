package com.carshare.repository

import com.carshare.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired lateinit var em: TestEntityManager
    @Autowired lateinit var customerRepository: CustomerRepository
    @Autowired lateinit var vehicleRepository: VehicleRepository
    @Autowired lateinit var damageReportRepository: DamageReportRepository
    @Autowired lateinit var reservationRepository: ReservationRepository
    @Autowired lateinit var rentalRepository: RentalRepository
    @Autowired lateinit var invoiceRepository: InvoiceRepository
    @Autowired lateinit var dailyUsageRepository: CustomerDailyReservationUsageRepository

    private lateinit var customer: Customer
    private lateinit var vehicle: Vehicle

    @BeforeEach
    fun setUp() {
        customer = em.persist(Customer(
            oauthSubject = "oauth|abc",
            email = "test@example.com",
            fullName = "Test User",
            stripeCustomerId = "cus_123",
            kycStatus = KycStatus.APPROVED
        ))
        vehicle = em.persist(Vehicle(
            smartcarId = "sc_001",
            licensePlate = "AB-12-CD",
            vehicleClass = VehicleClass.CITY_SMALL,
            status = VehicleStatus.AVAILABLE,
            batteryLevel = 80,
            city = "Amsterdam"
        ))
        em.flush()
    }

    // ---- CustomerRepository ----

    @Test
    fun `findByOauthSubject returns correct customer`() {
        val found = customerRepository.findByOauthSubject("oauth|abc")
        assertNotNull(found)
        assertEquals(customer.id, found!!.id)
    }

    @Test
    fun `findByOauthSubject returns null for unknown subject`() {
        assertNull(customerRepository.findByOauthSubject("oauth|unknown"))
    }

    @Test
    fun `findByEmail returns correct customer`() {
        val found = customerRepository.findByEmail("test@example.com")
        assertNotNull(found)
        assertEquals(customer.id, found!!.id)
    }

    // ---- VehicleRepository ----

    @Test
    fun `findByStatus returns only vehicles with matching status`() {
        em.persist(Vehicle(
            smartcarId = "sc_002", licensePlate = "EF-34-GH",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.RENTED
        ))
        em.flush()

        val available = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)
        assertTrue(available.all { it.status == VehicleStatus.AVAILABLE })
        assertTrue(available.any { it.id == vehicle.id })
        assertTrue(available.none { it.status == VehicleStatus.RENTED })
    }

    @Test
    fun `findByStatusAndVehicleClass filters correctly`() {
        em.persist(Vehicle(
            smartcarId = "sc_003", licensePlate = "IJ-56-KL",
            vehicleClass = VehicleClass.LONG_RANGE, status = VehicleStatus.AVAILABLE
        ))
        em.flush()

        val citySmall = vehicleRepository.findByStatusAndVehicleClass(
            VehicleStatus.AVAILABLE, VehicleClass.CITY_SMALL
        )
        assertTrue(citySmall.all { it.vehicleClass == VehicleClass.CITY_SMALL })
        assertTrue(citySmall.none { it.vehicleClass == VehicleClass.LONG_RANGE })
    }

    @Test
    fun `findByStatusAndCity filters by city`() {
        em.persist(Vehicle(
            smartcarId = "sc_004", licensePlate = "MN-78-OP",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.AVAILABLE,
            city = "Berlin"
        ))
        em.flush()

        val amsterdam = vehicleRepository.findByStatusAndCity(VehicleStatus.AVAILABLE, "Amsterdam")
        assertTrue(amsterdam.all { it.city == "Amsterdam" })
        assertTrue(amsterdam.none { it.city == "Berlin" })
    }

    @Test
    fun `findBySmartcarId returns correct vehicle`() {
        val found = vehicleRepository.findBySmartcarId("sc_001")
        assertNotNull(found)
        assertEquals(vehicle.id, found!!.id)
    }

    @Test
    fun `findBySmartcarId returns null for unknown id`() {
        assertNull(vehicleRepository.findBySmartcarId("sc_unknown"))
    }

    @Test
    fun `findByBatteryLevelLessThanAndStatus finds low battery available vehicles`() {
        em.persist(Vehicle(
            smartcarId = "sc_low", licensePlate = "QR-99-ST",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.AVAILABLE,
            batteryLevel = 10
        ))
        em.persist(Vehicle(
            smartcarId = "sc_ok", licensePlate = "UV-99-WX",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.AVAILABLE,
            batteryLevel = 90
        ))
        em.flush()

        val lowBattery = vehicleRepository.findByBatteryLevelLessThanAndStatus(15, VehicleStatus.AVAILABLE)
        assertTrue(lowBattery.all { (it.batteryLevel ?: 0) < 15 })
        assertTrue(lowBattery.none { (it.batteryLevel ?: 0) >= 15 })
    }

    // ---- DamageReportRepository ----

    @Test
    fun `findByVehicleIdOrderByCreatedAtDesc returns reports newest first`() {
        val older = em.persist(DamageReport(
            vehicle = vehicle, reportedBy = customer,
            position = DamagePosition.FRONT,
            photoUrl = "https://s3.example.com/old.jpg",
            licensePlatePhotoUrl = "https://s3.example.com/plate1.jpg",
            createdAt = Instant.now().minus(10, ChronoUnit.MINUTES)
        ))
        val newer = em.persist(DamageReport(
            vehicle = vehicle, reportedBy = customer,
            position = DamagePosition.BACK,
            photoUrl = "https://s3.example.com/new.jpg",
            licensePlatePhotoUrl = "https://s3.example.com/plate2.jpg",
            createdAt = Instant.now()
        ))
        em.flush()

        val reports = damageReportRepository.findByVehicleIdOrderByCreatedAtDesc(vehicle.id)
        assertEquals(2, reports.size)
        assertEquals(newer.id, reports[0].id)
        assertEquals(older.id, reports[1].id)
    }

    @Test
    fun `findByVehicleIdOrderByCreatedAtDesc returns empty list for vehicle with no reports`() {
        val otherVehicle = em.persist(Vehicle(
            smartcarId = "sc_clean", licensePlate = "ZZ-00-AA",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.AVAILABLE
        ))
        em.flush()

        val reports = damageReportRepository.findByVehicleIdOrderByCreatedAtDesc(otherVehicle.id)
        assertTrue(reports.isEmpty())
    }

    // ---- ReservationRepository ----

    @Test
    fun `findByCustomerIdAndStatus returns only matching reservations`() {
        val active = em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plus(20, ChronoUnit.MINUTES)
        ))
        em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.CANCELLED,
            expiresAt = Instant.now().minus(5, ChronoUnit.MINUTES)
        ))
        em.flush()

        val results = reservationRepository.findByCustomerIdAndStatus(customer.id, ReservationStatus.ACTIVE)
        assertEquals(1, results.size)
        assertEquals(active.id, results[0].id)
    }

    @Test
    fun `findByVehicleIdAndStatus returns only reservations for that vehicle`() {
        val otherVehicle = em.persist(Vehicle(
            smartcarId = "sc_other", licensePlate = "OT-00-HE",
            vehicleClass = VehicleClass.CITY_SMALL, status = VehicleStatus.RESERVED
        ))
        val reservation = em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plus(20, ChronoUnit.MINUTES)
        ))
        em.flush()

        val results = reservationRepository.findByVehicleIdAndStatus(vehicle.id, ReservationStatus.ACTIVE)
        assertEquals(1, results.size)
        assertEquals(reservation.id, results[0].id)

        val otherResults = reservationRepository.findByVehicleIdAndStatus(otherVehicle.id, ReservationStatus.ACTIVE)
        assertTrue(otherResults.isEmpty())
    }

    @Test
    fun `findExpiredReservations returns only ACTIVE reservations past their expiry`() {
        val expired = em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().minus(5, ChronoUnit.MINUTES)
        ))
        em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.ACTIVE,
            expiresAt = Instant.now().plus(20, ChronoUnit.MINUTES)
        ))
        em.persist(Reservation(
            customer = customer, vehicle = vehicle,
            status = ReservationStatus.CANCELLED,
            expiresAt = Instant.now().minus(10, ChronoUnit.MINUTES)
        ))
        em.flush()

        val results = reservationRepository.findExpiredReservations(Instant.now())
        assertEquals(1, results.size)
        assertEquals(expired.id, results[0].id)
    }

    // ---- RentalRepository ----

    @Test
    fun `findByCustomerIdAndStatus returns active rentals for customer`() {
        val activeRental = em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startedAt = Instant.now().minus(30, ChronoUnit.MINUTES)
        ))
        em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.COMPLETED,
            startedAt = Instant.now().minus(2, ChronoUnit.HOURS),
            endedAt = Instant.now().minus(1, ChronoUnit.HOURS)
        ))
        em.flush()

        val results = rentalRepository.findByCustomerIdAndStatus(customer.id, RentalStatus.ACTIVE)
        assertEquals(1, results.size)
        assertEquals(activeRental.id, results[0].id)
    }

    @Test
    fun `findByVehicleIdAndStatus returns the active rental for a vehicle`() {
        val rental = em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startedAt = Instant.now().minus(10, ChronoUnit.MINUTES)
        ))
        em.flush()

        val found = rentalRepository.findByVehicleIdAndStatus(vehicle.id, RentalStatus.ACTIVE)
        assertNotNull(found)
        assertEquals(rental.id, found!!.id)
    }

    @Test
    fun `findByVehicleIdAndStatus returns null when no active rental`() {
        val found = rentalRepository.findByVehicleIdAndStatus(vehicle.id, RentalStatus.ACTIVE)
        assertNull(found)
    }

    // ---- InvoiceRepository ----

    @Test
    fun `findByCustomerId returns all invoices for customer`() {
        val rental = em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.COMPLETED,
            startedAt = Instant.now().minus(1, ChronoUnit.HOURS),
            endedAt = Instant.now()
        ))
        em.persist(Invoice(customer = customer, rental = rental, type = InvoiceType.RENTAL, totalCents = 390))
        em.persist(Invoice(customer = customer, rental = rental, type = InvoiceType.RENTAL, totalCents = 780))
        em.flush()

        val invoices = invoiceRepository.findByCustomerId(customer.id)
        assertEquals(2, invoices.size)
        assertTrue(invoices.all { it.customer.id == customer.id })
    }

    @Test
    fun `findByRentalId returns the invoice for a rental`() {
        val rental = em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.COMPLETED,
            startedAt = Instant.now().minus(1, ChronoUnit.HOURS),
            endedAt = Instant.now()
        ))
        val invoice = em.persist(Invoice(
            customer = customer, rental = rental,
            type = InvoiceType.RENTAL, totalCents = 1560
        ))
        em.flush()

        val found = invoiceRepository.findByRentalId(rental.id)
        assertNotNull(found)
        assertEquals(invoice.id, found!!.id)
    }

    @Test
    fun `findByRentalId returns null when no invoice exists for rental`() {
        val rental = em.persist(Rental(
            customer = customer, vehicle = vehicle,
            status = RentalStatus.ACTIVE,
            startedAt = Instant.now()
        ))
        em.flush()

        assertNull(invoiceRepository.findByRentalId(rental.id))
    }

    // ---- CustomerDailyReservationUsageRepository ----

    @Test
    fun `findByCustomerIdAndUsageDate returns usage for correct date`() {
        val today = LocalDate.now()
        val usage = em.persist(CustomerDailyReservationUsage(
            customer = customer, usageDate = today, freeMinutesUsed = 15
        ))
        em.flush()

        val found = dailyUsageRepository.findByCustomerIdAndUsageDate(customer.id, today)
        assertNotNull(found)
        assertEquals(usage.id, found!!.id)
        assertEquals(15, found.freeMinutesUsed)
    }

    @Test
    fun `findByCustomerIdAndUsageDate returns null for different date`() {
        em.persist(CustomerDailyReservationUsage(
            customer = customer, usageDate = LocalDate.now().minusDays(1), freeMinutesUsed = 20
        ))
        em.flush()

        val found = dailyUsageRepository.findByCustomerIdAndUsageDate(customer.id, LocalDate.now())
        assertNull(found)
    }
}
