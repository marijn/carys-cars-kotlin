package com.carshare.controller

import com.carshare.domain.*
import com.carshare.modules.LicensePlate
import com.carshare.modules.reserving.AnyReservationCommand
import com.carshare.repository.VehicleRepository
import com.carshare.service.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

// ---- DTOs ----

data class RegisterRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val fullName: String
)

data class PaymentMethodRequest(@field:NotBlank val paymentMethodId: String)

data class DamageReportRequest(
    @field:NotBlank val position: String,
    @field:NotBlank val photoUrl: String,
    @field:NotBlank val licensePlatePhotoUrl: String,
    val notes: String? = null
)

data class ExtendReservationRequest(val additionalMinutes: Long = 10)

// ---- Controllers ----

@RestController
@RequestMapping("/api/customers")
class CustomerController(
    private val customerService: CustomerService
) {
    @PostMapping("/register")
    fun register(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody req: RegisterRequest
    ) = customerService.getOrCreateCustomer(jwt.subject, req.email, req.fullName)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): Customer {
        return customerService.getOrCreateCustomer(jwt.subject,
            jwt.getClaim("email") ?: "",
            jwt.getClaim("name") ?: "")
    }

    @PostMapping("/me/payment-method")
    fun addPaymentMethod(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody req: PaymentMethodRequest
    ): ResponseEntity<Void> {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        customerService.addPaymentMethod(customer.id, req.paymentMethodId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/me/kyc/start")
    fun startKyc(@AuthenticationPrincipal jwt: Jwt): Map<String, String> {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        val url = customerService.initiateKyc(customer.id)
        return mapOf("veriffUrl" to url)
    }
}

@RestController
@RequestMapping("/api/kyc")
class KycCallbackController(private val customerService: CustomerService) {
    @PostMapping("/callback")
    fun callback(@RequestBody body: Map<String, Any>): ResponseEntity<Void> {
        val verification = body["verification"] as? Map<*, *> ?: return ResponseEntity.badRequest().build()
        val sessionId = verification["id"]?.toString() ?: return ResponseEntity.badRequest().build()
        val status = verification["status"]?.toString() ?: return ResponseEntity.badRequest().build()
        customerService.handleKycCallback(sessionId, status)
        return ResponseEntity.ok().build()
    }
}

@RestController
@RequestMapping("/api/vehicles")
class VehicleController(
    private val vehicleService: VehicleService,
    private val customerService: CustomerService
) {
    @GetMapping
    fun listVehicles(
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) vehicleClass: VehicleClass?
    ) = vehicleService.getAvailableVehicles(city, vehicleClass)

    @GetMapping("/{id}")
    fun getVehicle(@PathVariable id: UUID) = vehicleService.getVehicle(id)

    @GetMapping("/{id}/damage-reports")
    fun getDamageReports(@PathVariable id: UUID) = vehicleService.getDamageReports(id)

    @PostMapping("/{id}/damage-reports")
    fun reportDamage(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @Valid @RequestBody req: DamageReportRequest
    ): DamageReport {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        return vehicleService.reportDamage(
            vehicleId = id,
            customer = customer,
            position = DamagePosition.valueOf(req.position.uppercase()),
            photoUrl = req.photoUrl,
            licensePlatePhotoUrl = req.licensePlatePhotoUrl,
            notes = req.notes
        )
    }
}

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService,
    private val customerService: CustomerService,
) {
    @PostMapping
    fun createReservation(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam vehicleId: UUID
    ): Reservation {
        AnyReservationCommand.PleaseReserveVehicle(
            LicensePlate.DutchLicensePlate("GHX-12-A"),
            extractFromJwt(jwt),
            LocalDateTime.now()
        );

        val customer = customerService.getOrCreateCustomer(
            jwt.subject,
            "", ""
        )
        return reservationService.createReservation(
            customer.id,
            vehicleId
        )
    }

    private fun extractFromJwt(jwt: Jwt): String {
        // TODO: take customer id from jwt
        return "customer:11111111-1111-1111-1111-111111111111"
    }

    @PostMapping("/{id}/extend")
    fun extendReservation(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody req: ExtendReservationRequest
    ): Reservation {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        return reservationService.extendReservation(id, customer.id, req.additionalMinutes)
    }

    @DeleteMapping("/{id}")
    fun cancelReservation(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID
    ): Reservation {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        return reservationService.cancelReservation(id, customer.id)
    }
}

@RestController
@RequestMapping("/api/rentals")
class RentalController(
    private val rentalService: RentalService,
    private val customerService: CustomerService
) {
    @PostMapping("/start")
    fun startRental(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam reservationId: UUID
    ): Rental {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        return rentalService.startRental(customer.id, reservationId)
    }

    @PostMapping("/{id}/end")
    fun endRental(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID
    ): Invoice {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        return rentalService.endRental(id, customer.id)
    }

    @GetMapping("/active")
    fun getActiveRental(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Rental> {
        val customer = customerService.getOrCreateCustomer(jwt.subject, "", "")
        val rental = rentalService.getActiveRental(customer.id)
        return if (rental != null) ResponseEntity.ok(rental) else ResponseEntity.noContent().build()
    }
}
