package com.carshare.service

import com.carshare.domain.*
import com.carshare.external.ISmartcarClient
import com.carshare.external.IStripeClient
import com.carshare.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.*

// Haversine distance between two lat/lon points in km — used for operating region check only
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@Service
class RentalService(
    private val rentalRepository: RentalRepository,
    private val vehicleRepository: VehicleRepository,
    private val invoiceRepository: InvoiceRepository,
    private val reservationService: ReservationService,
    private val customerService: CustomerService,
    private val smartcarClient: ISmartcarClient,
    private val stripeClient: IStripeClient,
    @Value("\${app.rental.rate-cents-per-minute}") private val ratePerMinute: Int,
    @Value("\${app.rental.max-km-free}") private val maxKmFree: Int,
    @Value("\${app.rental.overage-rate-cents-per-km}") private val overageRatePerKm: Int
) {
    @Transactional
    fun startRental(customerId: UUID, reservationId: UUID): Rental {
        val customer = customerService.getCustomer(customerId)
        check(customerService.isReadyToRent(customer)) { "Customer not ready to rent" }

        val reservation = reservationService.convertToRental(reservationId, customerId)
        val vehicle = reservation.vehicle

        val overageCost = reservationService.computeReservationCost(reservation, customerId)
        reservationService.consumeFreeMinutes(customerId, reservation.freeMinutesUsed)

        val startOdometer = smartcarClient.getOdometer(vehicle.smartcarId)
        smartcarClient.unlockVehicle(vehicle.smartcarId)

        vehicle.status = VehicleStatus.RENTED
        vehicleRepository.save(vehicle)

        val rental = rentalRepository.save(Rental(
            customer = customer,
            vehicle = vehicle,
            reservation = reservation,
            startOdometerKm = startOdometer
        ))

        if (overageCost > 0) {
            issueAndChargeInvoice(Invoice(
                customer = customer,
                reservation = reservation,
                type = InvoiceType.RESERVATION_OVERAGE,
                reservationOverageCostCents = overageCost,
                totalCents = overageCost
            ), customer)
        }

        return rental
    }

    @Transactional
    fun endRental(rentalId: UUID, customerId: UUID): Invoice {
        val rental = rentalRepository.findById(rentalId)
            .orElseThrow { IllegalArgumentException("Rental not found") }
        check(rental.customer.id == customerId) { "Not your rental" }
        check(rental.status == RentalStatus.ACTIVE) { "Rental is not active" }

        val vehicle = rental.vehicle
        val location = smartcarClient.getLocation(vehicle.smartcarId)
        val endOdometer = smartcarClient.getOdometer(vehicle.smartcarId)

        val endCity = location?.let { resolveCity(it.latitude, it.longitude) }
        checkNotNull(endCity) { "Cannot end rental outside operating region" }

        // After checkNotNull(endCity), location is guaranteed non-null
        val resolvedLocation = location!!

        smartcarClient.lockVehicle(vehicle.smartcarId)

        // Only set endedAt if not already set — preserves test-supplied timestamps
        val endedAt = rental.endedAt ?: Instant.now()
        rental.status = RentalStatus.COMPLETED
        rental.endedAt = endedAt
        rental.endOdometerKm = endOdometer
        rentalRepository.save(rental)

        vehicle.status = VehicleStatus.AVAILABLE
        vehicle.latitude = resolvedLocation.latitude
        vehicle.longitude = resolvedLocation.longitude
        vehicle.city = endCity
        endOdometer?.let { vehicle.odometerKm = it }
        vehicleRepository.save(vehicle)

        return issueRentalInvoice(rental, customerId)
    }

    private fun issueRentalInvoice(rental: Rental, customerId: UUID): Invoice {
        val customer = customerService.getCustomer(customerId)

        val startedAt = rental.startedAt
        val endedAt = rental.endedAt ?: Instant.now()
        val durationMinutes = ChronoUnit.MINUTES.between(startedAt, endedAt).toInt().coerceAtLeast(1)
        val rentalCost = durationMinutes * ratePerMinute

        val startOdometer = rental.startOdometerKm
        val endOdometer = rental.endOdometerKm
        val distanceKm = if (startOdometer != null && endOdometer != null) {
            (endOdometer - startOdometer).coerceAtLeast(0.0)
        } else 0.0

        val distanceOverage = if (distanceKm > maxKmFree) {
            ((distanceKm - maxKmFree) * overageRatePerKm).toInt()
        } else 0

        val invoice = Invoice(
            customer = customer,
            rental = rental,
            type = InvoiceType.RENTAL,
            rentalMinutes = durationMinutes,
            rentalCostCents = rentalCost,
            distanceKm = distanceKm,
            distanceOverageCostCents = distanceOverage,
            totalCents = rentalCost + distanceOverage
        )

        return issueAndChargeInvoice(invoice, customer)
    }

    private fun issueAndChargeInvoice(invoice: Invoice, customer: Customer): Invoice {
        val saved = invoiceRepository.save(invoice)
        if (saved.totalCents == 0) {
            saved.status = InvoiceStatus.PAID
            return invoiceRepository.save(saved)
        }

        return try {
            val paymentIntent = stripeClient.chargeCustomer(
                stripeCustomerId = customer.stripeCustomerId!!,
                paymentMethodId = customer.stripePaymentMethodId!!,
                amountCents = saved.totalCents,
                description = "CarShare invoice ${saved.id}"
            )
            saved.stripePaymentIntentId = paymentIntent.id
            saved.status = InvoiceStatus.PAID
            saved.paidAt = Instant.now()
            invoiceRepository.save(saved)
        } catch (e: Exception) {
            saved.status = InvoiceStatus.FAILED
            invoiceRepository.save(saved)
        }
    }

    fun getActiveRental(customerId: UUID): Rental? =
        rentalRepository.findByCustomerIdAndStatus(customerId, RentalStatus.ACTIVE).firstOrNull()

    private fun resolveCity(lat: Double, lon: Double): String? {
        val cityCoordinates = mapOf(
            "Amsterdam"  to Pair(52.3676, 4.9041),
            "Berlin"     to Pair(52.5200, 13.4050),
            "Zurich"     to Pair(47.3769, 8.5417),
            "Munich"     to Pair(48.1351, 11.5820),
            "Copenhagen" to Pair(55.6761, 12.5683),
            "Rotterdam"  to Pair(51.9244, 4.4777),
            "Dusseldorf" to Pair(51.2217, 6.7762)
        )
        return cityCoordinates.entries
            .minByOrNull { (_, coords) -> haversineKm(lat, lon, coords.first, coords.second) }
            ?.takeIf { (_, coords) -> haversineKm(lat, lon, coords.first, coords.second) < 30.0 }
            ?.key
    }
}
