package com.carshare.service

import com.carshare.domain.*
import com.carshare.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val vehicleRepository: VehicleRepository,
    private val customerService: CustomerService,
    private val dailyUsageRepository: CustomerDailyReservationUsageRepository,
    @Value("\${app.reservation.free-minutes-per-day}") private val freeMinutesPerDay: Int,
    @Value("\${app.reservation.expiry-minutes}") private val expiryMinutes: Long,
    @Value("\${app.reservation.overage-rate-cents}") private val overageRateCents: Int
) {
    @Transactional
    fun createReservation(customerId: UUID, vehicleId: UUID): Reservation {
        val customer = customerService.getCustomer(customerId)
        check(customerService.isReadyToRent(customer)) { "Customer not ready to rent (KYC or payment missing)" }

        val vehicle = vehicleRepository.findById(vehicleId).orElseThrow { IllegalArgumentException("Vehicle not found") }
        check(vehicle.status == VehicleStatus.AVAILABLE) { "Vehicle is not available" }

        // Check no existing active reservation
        val existing = reservationRepository.findByCustomerIdAndStatus(customerId, ReservationStatus.ACTIVE)
        check(existing.isEmpty()) { "Customer already has an active reservation" }

        vehicle.status = VehicleStatus.RESERVED
        vehicleRepository.save(vehicle)

        return reservationRepository.save(Reservation(
            customer = customer,
            vehicle = vehicle,
            expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES)
        ))
    }

    @Transactional
    fun extendReservation(reservationId: UUID, customerId: UUID, additionalMinutes: Long): Reservation {
        val reservation = getActiveReservation(reservationId, customerId)
        reservation.extendedUntil = (reservation.extendedUntil ?: reservation.expiresAt)
            .plus(additionalMinutes, ChronoUnit.MINUTES)
        reservation.expiresAt = reservation.extendedUntil!!
        return reservationRepository.save(reservation)
    }

    @Transactional
    fun cancelReservation(reservationId: UUID, customerId: UUID): Reservation {
        val reservation = getActiveReservation(reservationId, customerId)
        return endReservation(reservation, ReservationStatus.CANCELLED)
    }

    @Transactional
    fun convertToRental(reservationId: UUID, customerId: UUID): Reservation {
        val reservation = getActiveReservation(reservationId, customerId)
        return endReservation(reservation, ReservationStatus.CONVERTED)
    }

    fun computeReservationCost(reservation: Reservation, customerId: UUID): Int {
        val durationMinutes = ChronoUnit.MINUTES.between(reservation.reservedAt, Instant.now()).toInt()
        val dailyUsage = getDailyUsage(customerId)
        val remainingFree = (freeMinutesPerDay - dailyUsage.freeMinutesUsed).coerceAtLeast(0)
        val freeUsed = durationMinutes.coerceAtMost(remainingFree)
        val overageMinutes = (durationMinutes - freeUsed).coerceAtLeast(0)
        reservation.freeMinutesUsed = freeUsed
        reservation.overageMinutes = overageMinutes
        reservation.overageCostCents = overageMinutes * overageRateCents
        return reservation.overageCostCents
    }

    @Transactional
    fun consumeFreeMinutes(customerId: UUID, minutesUsed: Int) {
        val usage = getDailyUsage(customerId)
        usage.freeMinutesUsed = (usage.freeMinutesUsed + minutesUsed).coerceAtMost(freeMinutesPerDay)
        dailyUsageRepository.save(usage)
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun expireOldReservations() {
        reservationRepository.findExpiredReservations(Instant.now()).forEach { reservation ->
            endReservation(reservation, ReservationStatus.EXPIRED)
        }
    }

    private fun endReservation(reservation: Reservation, status: ReservationStatus): Reservation {
        reservation.status = status
        reservation.endedAt = Instant.now()
        reservation.vehicle.status = VehicleStatus.AVAILABLE
        vehicleRepository.save(reservation.vehicle)
        return reservationRepository.save(reservation)
    }

    private fun getActiveReservation(reservationId: UUID, customerId: UUID): Reservation {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { IllegalArgumentException("Reservation not found") }
        check(reservation.customer.id == customerId) { "Not your reservation" }
        check(reservation.status == ReservationStatus.ACTIVE) { "Reservation is not active" }
        return reservation
    }

    private fun getDailyUsage(customerId: UUID): CustomerDailyReservationUsage {
        val customer = customerService.getCustomer(customerId)
        return dailyUsageRepository.findByCustomerIdAndUsageDate(customerId, LocalDate.now())
            ?: dailyUsageRepository.save(CustomerDailyReservationUsage(customer = customer))
    }
}
