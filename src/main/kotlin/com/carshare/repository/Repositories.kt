package com.carshare.repository

import com.carshare.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByOauthSubject(subject: String): Customer?
    fun findByEmail(email: String): Customer?
}

interface VehicleRepository : JpaRepository<Vehicle, UUID> {
    fun findByStatus(status: VehicleStatus): List<Vehicle>
    fun findByStatusAndVehicleClass(status: VehicleStatus, vehicleClass: VehicleClass): List<Vehicle>
    fun findByStatusAndCity(status: VehicleStatus, city: String): List<Vehicle>
    fun findBySmartcarId(smartcarId: String): Vehicle?
    fun findByBatteryLevelLessThanAndStatus(threshold: Int, status: VehicleStatus): List<Vehicle>
}

interface DamageReportRepository : JpaRepository<DamageReport, UUID> {
    fun findByVehicleIdOrderByCreatedAtDesc(vehicleId: UUID): List<DamageReport>
}

interface ReservationRepository : JpaRepository<Reservation, UUID> {
    fun findByCustomerIdAndStatus(customerId: UUID, status: ReservationStatus): List<Reservation>
    fun findByVehicleIdAndStatus(vehicleId: UUID, status: ReservationStatus): List<Reservation>

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :now")
    fun findExpiredReservations(now: Instant): List<Reservation>
}

interface RentalRepository : JpaRepository<Rental, UUID> {
    fun findByCustomerIdAndStatus(customerId: UUID, status: RentalStatus): List<Rental>
    fun findByVehicleIdAndStatus(vehicleId: UUID, status: RentalStatus): Rental?
}

interface InvoiceRepository : JpaRepository<Invoice, UUID> {
    fun findByCustomerId(customerId: UUID): List<Invoice>
    fun findByRentalId(rentalId: UUID): Invoice?
}

interface CustomerDailyReservationUsageRepository : JpaRepository<CustomerDailyReservationUsage, UUID> {
    fun findByCustomerIdAndUsageDate(customerId: UUID, date: LocalDate): CustomerDailyReservationUsage?
}
