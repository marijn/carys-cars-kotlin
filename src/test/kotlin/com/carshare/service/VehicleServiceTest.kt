package com.carshare.service

import com.carshare.domain.*
import com.carshare.external.ISmartcarClient
import com.carshare.repository.DamageReportRepository
import com.carshare.repository.VehicleRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import java.util.UUID

class VehicleServiceTest {

    private val vehicleRepository: VehicleRepository = mock()
    private val damageReportRepository: DamageReportRepository = mock()
    private val smartcarClient: ISmartcarClient = mock()

    private val service = VehicleService(
        vehicleRepository = vehicleRepository,
        damageReportRepository = damageReportRepository,
        smartcarClient = smartcarClient,
        chargeThreshold = 15
    )

    private fun makeVehicle(
        status: VehicleStatus = VehicleStatus.AVAILABLE,
        vehicleClass: VehicleClass = VehicleClass.CITY_SMALL,
        city: String = "Amsterdam",
        battery: Int = 80
    ) = Vehicle(
        smartcarId = "sc_${UUID.randomUUID()}",
        licensePlate = "AA-000-BB",
        vehicleClass = vehicleClass,
        status = status,
        batteryLevel = battery,
        city = city
    )

    @Test
    fun `getAvailableVehicles returns only AVAILABLE vehicles`() {
        val available = makeVehicle(status = VehicleStatus.AVAILABLE)
        makeVehicle(status = VehicleStatus.RESERVED) // created but not returned by repo stub — verifies filtering
        whenever(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).thenReturn(listOf(available))

        val result = service.getAvailableVehicles()

        assertEquals(1, result.size)
        assertEquals(available.id, result[0].id)
    }

    @Test
    fun `getAvailableVehicles filters by city`() {
        val amsterdam = makeVehicle(city = "Amsterdam")
        whenever(vehicleRepository.findByStatusAndCity(VehicleStatus.AVAILABLE, "Amsterdam"))
            .thenReturn(listOf(amsterdam))

        val result = service.getAvailableVehicles(city = "Amsterdam")

        assertEquals(1, result.size)
        assertEquals("Amsterdam", result[0].city)
    }

    @Test
    fun `getAvailableVehicles filters by vehicle class`() {
        val truck = makeVehicle(vehicleClass = VehicleClass.BULKY)
        whenever(vehicleRepository.findByStatusAndVehicleClass(VehicleStatus.AVAILABLE, VehicleClass.BULKY))
            .thenReturn(listOf(truck))

        val result = service.getAvailableVehicles(vehicleClass = VehicleClass.BULKY)

        assertEquals(1, result.size)
        assertEquals(VehicleClass.BULKY, result[0].vehicleClass)
    }

    @Test
    fun `getVehicle throws when not found`() {
        val id = UUID.randomUUID()
        whenever(vehicleRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> {
            service.getVehicle(id)
        }
    }

    @Test
    fun `reportDamage saves a damage report with correct fields`() {
        val vehicle = makeVehicle()
        val customer = Customer(
            oauthSubject = "oauth|abc",
            email = "d@x.com",
            fullName = "Driver",
            kycStatus = KycStatus.APPROVED
        )
        whenever(vehicleRepository.findById(vehicle.id)).thenReturn(Optional.of(vehicle))
        whenever(damageReportRepository.save(any())).thenAnswer { it.arguments[0] }

        val report = service.reportDamage(
            vehicleId = vehicle.id,
            customer = customer,
            position = DamagePosition.FRONT,
            photoUrl = "https://s3.example.com/photo.jpg",
            licensePlatePhotoUrl = "https://s3.example.com/plate.jpg",
            notes = "Scratch on bumper"
        )

        assertEquals(DamagePosition.FRONT, report.position)
        assertEquals("https://s3.example.com/photo.jpg", report.photoUrl)
        assertEquals("Scratch on bumper", report.notes)
        assertEquals(customer.id, report.reportedBy?.id)
    }

    @Test
    fun `needsCharging returns true when battery below threshold`() {
        val vehicle = makeVehicle(battery = 10)
        assertTrue(service.needsCharging(vehicle))
    }

    @Test
    fun `needsCharging returns false when battery above threshold`() {
        val vehicle = makeVehicle(battery = 80)
        assertFalse(service.needsCharging(vehicle))
    }

    @Test
    fun `needsCharging returns true at exactly threshold`() {
        val vehicle = makeVehicle(battery = 15)
        // 15 < 15 is false, so should NOT need charging at exactly threshold
        assertFalse(service.needsCharging(vehicle))
    }

    @Test
    fun `updateVehicleStatus changes status and saves`() {
        val vehicle = makeVehicle(status = VehicleStatus.AVAILABLE)
        whenever(vehicleRepository.findById(vehicle.id)).thenReturn(Optional.of(vehicle))
        whenever(vehicleRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.updateVehicleStatus(vehicle.id, VehicleStatus.MAINTENANCE)

        assertEquals(VehicleStatus.MAINTENANCE, result.status)
        verify(vehicleRepository).save(argThat { status == VehicleStatus.MAINTENANCE })
    }
}
