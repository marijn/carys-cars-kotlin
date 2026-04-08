package com.carshare.domain

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class KycStatus { PENDING, APPROVED, REJECTED }
enum class VehicleStatus { AVAILABLE, RESERVED, RENTED, CHARGING, MAINTENANCE }
enum class VehicleClass { BULKY, CITY_SMALL, LONG_RANGE, FUN_PREMIUM }
enum class ReservationStatus { ACTIVE, CONVERTED, CANCELLED, EXPIRED }
enum class RentalStatus { ACTIVE, COMPLETED }
enum class InvoiceStatus { PENDING, PAID, FAILED }
enum class InvoiceType { RENTAL, RESERVATION_OVERAGE }
enum class DamagePosition { FRONT, BACK, LEFT, RIGHT }

@Entity @Table(name = "customers")
data class Customer(
    @Id val id: UUID = UUID.randomUUID(),
    val oauthSubject: String = "",
    val email: String = "",
    val fullName: String = "",
    var stripeCustomerId: String? = null,
    var stripePaymentMethodId: String? = null,
    @Enumerated(EnumType.STRING) var kycStatus: KycStatus = KycStatus.PENDING,
    var veriffSessionId: String? = null,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)

@Entity @Table(name = "vehicles")
data class Vehicle(
    @Id val id: UUID = UUID.randomUUID(),
    val smartcarId: String = "",
    val licensePlate: String = "",
    val make: String = "",
    val model: String = "",
    @Column(name = "model_year") val year: Int = 2023,
    /**
     * ACRISS 4-character classification code.
     * Position 1: Category (size/prestige) — e.g. M=Mini, C=Compact, I=Intermediate, F=Fullsize, P=Premium, L=Luxury
     * Position 2: Type (body style) — e.g. D=4-5 Door, W=Wagon/Estate, F=SUV, T=Convertible, G=Crossover, V=Van
     * Position 3: Transmission — always A (Automatic) for our fleet
     * Position 4: Fuel — always E (Electric/BEV) for our fleet
     * Example: CDAE = Compact, 4-door, Automatic, Electric
     */
    val acrissCode: String = "",
    @Enumerated(EnumType.STRING) val vehicleClass: VehicleClass = VehicleClass.CITY_SMALL,
    @Enumerated(EnumType.STRING) var status: VehicleStatus = VehicleStatus.AVAILABLE,
    var batteryLevel: Int? = null,
    var odometerKm: Double? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var city: String? = null,
    var country: String? = null,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)

@Entity @Table(name = "damage_reports")
data class DamageReport(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "vehicle_id") val vehicle: Vehicle = Vehicle(),
    @ManyToOne @JoinColumn(name = "reported_by_customer_id") val reportedBy: Customer? = null,
    @Enumerated(EnumType.STRING) val position: DamagePosition = DamagePosition.FRONT,
    val photoUrl: String = "",
    val licensePlatePhotoUrl: String = "",
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

@Entity @Table(name = "reservations")
data class Reservation(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "customer_id") val customer: Customer = Customer(),
    @ManyToOne @JoinColumn(name = "vehicle_id") val vehicle: Vehicle = Vehicle(),
    @Enumerated(EnumType.STRING) var status: ReservationStatus = ReservationStatus.ACTIVE,
    var freeMinutesUsed: Int = 0,
    var overageMinutes: Int = 0,
    var overageCostCents: Int = 0,
    val reservedAt: Instant = Instant.now(),
    var expiresAt: Instant = Instant.now(),
    var endedAt: Instant? = null,
    var extendedUntil: Instant? = null
)

@Entity @Table(name = "rentals")
data class Rental(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "customer_id") val customer: Customer = Customer(),
    @ManyToOne @JoinColumn(name = "vehicle_id") val vehicle: Vehicle = Vehicle(),
    @ManyToOne @JoinColumn(name = "reservation_id") val reservation: Reservation? = null,
    @Enumerated(EnumType.STRING) var status: RentalStatus = RentalStatus.ACTIVE,
    val startOdometerKm: Double? = null,
    var endOdometerKm: Double? = null,
    val startedAt: Instant = Instant.now(),
    var endedAt: Instant? = null
)

@Entity @Table(name = "invoices")
data class Invoice(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "customer_id") val customer: Customer = Customer(),
    @ManyToOne @JoinColumn(name = "rental_id") val rental: Rental? = null,
    @ManyToOne @JoinColumn(name = "reservation_id") val reservation: Reservation? = null,
    @Enumerated(EnumType.STRING) val type: InvoiceType = InvoiceType.RENTAL,
    @Enumerated(EnumType.STRING) var status: InvoiceStatus = InvoiceStatus.PENDING,
    val rentalMinutes: Int? = null,
    val rentalCostCents: Int = 0,
    val distanceKm: Double? = null,
    val distanceOverageCostCents: Int = 0,
    val reservationOverageCostCents: Int = 0,
    val totalCents: Int = 0,
    var stripePaymentIntentId: String? = null,
    val issuedAt: Instant = Instant.now(),
    var paidAt: Instant? = null
)

@Entity @Table(name = "customer_daily_reservation_usage")
data class CustomerDailyReservationUsage(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "customer_id") val customer: Customer = Customer(),
    val usageDate: LocalDate = LocalDate.now(),
    var freeMinutesUsed: Int = 0
)
