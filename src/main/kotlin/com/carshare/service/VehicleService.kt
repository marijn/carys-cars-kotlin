package com.carshare.service

import com.carshare.domain.*
import com.carshare.external.ISmartcarClient
import com.carshare.repository.DamageReportRepository
import com.carshare.repository.VehicleRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class VehicleService(
    private val vehicleRepository: VehicleRepository,
    private val damageReportRepository: DamageReportRepository,
    private val smartcarClient: ISmartcarClient,
    @Value("\${app.battery.charge-threshold-percent}") private val chargeThreshold: Int
) {
    fun getAvailableVehicles(city: String? = null, vehicleClass: VehicleClass? = null): List<Vehicle> {
        return when {
            city != null && vehicleClass != null ->
                vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)
                    .filter { it.city == city && it.vehicleClass == vehicleClass }
            city != null ->
                vehicleRepository.findByStatusAndCity(VehicleStatus.AVAILABLE, city)
            vehicleClass != null ->
                vehicleRepository.findByStatusAndVehicleClass(VehicleStatus.AVAILABLE, vehicleClass)
            else ->
                vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)
        }
    }

    fun getVehicle(id: UUID): Vehicle =
        vehicleRepository.findById(id).orElseThrow { IllegalArgumentException("Vehicle not found") }

    fun getDamageReports(vehicleId: UUID): List<DamageReport> =
        damageReportRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId)

    @Transactional
    fun reportDamage(
        vehicleId: UUID,
        customer: Customer,
        position: DamagePosition,
        photoUrl: String,
        licensePlatePhotoUrl: String,
        notes: String?
    ): DamageReport {
        val vehicle = getVehicle(vehicleId)
        return damageReportRepository.save(DamageReport(
            vehicle = vehicle,
            reportedBy = customer,
            position = position,
            photoUrl = photoUrl,
            licensePlatePhotoUrl = licensePlatePhotoUrl,
            notes = notes
        ))
    }

    @Transactional
    fun updateVehicleStatus(vehicleId: UUID, status: VehicleStatus): Vehicle {
        val vehicle = getVehicle(vehicleId)
        vehicle.status = status
        vehicle.updatedAt = Instant.now()
        return vehicleRepository.save(vehicle)
    }

    @Transactional
    fun syncVehicleLocation(vehicle: Vehicle) {
        val location = smartcarClient.getLocation(vehicle.smartcarId) ?: return
        vehicle.latitude = location.latitude
        vehicle.longitude = location.longitude
        vehicle.updatedAt = Instant.now()
        vehicleRepository.save(vehicle)
    }

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    fun syncSignals() {
        vehicleRepository.findAll().forEach { vehicle ->
            val level = smartcarClient.getBatteryLevel(vehicle.smartcarId)
            val odometer = smartcarClient.getOdometer(vehicle.smartcarId)
            if (level == null && odometer == null) return@forEach
            level?.let {
                vehicle.batteryLevel = it
                if (it < chargeThreshold && vehicle.status == VehicleStatus.AVAILABLE) {
                    vehicle.status = VehicleStatus.CHARGING
                }
            }
            odometer?.let { vehicle.odometerKm = it }
            vehicle.updatedAt = Instant.now()
            vehicleRepository.save(vehicle)
        }
    }

    fun needsCharging(vehicle: Vehicle): Boolean =
        (vehicle.batteryLevel ?: 100) < chargeThreshold
}
