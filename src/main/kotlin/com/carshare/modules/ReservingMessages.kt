package com.carshare.modules

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import com.carshare.modules.reserving.ReservationRejectionReason
import java.time.LocalDateTime

sealed interface AnyReservationEvent: Event {
    data class VehicleEnteredOperation(
        /**
         * @example "DE:M-CC-0001"
         */
        val vehicle: LicensePlate,
        /**
         * @example "fun vehicles"
         */
        val vehicleClass: VehicleClass,
        /**
         * @example 2024-11-02 16:59:01 Europe/Amsterdam
         */
        val occurredOn: LocalDateTime
    ): AnyReservationEvent

    data class VehicleWasReserved(
        /**
         * @example "DE:M-CC-0001"
         */
        val vehicle: LicensePlate,

        /**
         * @example "fun vehicles"
         */
        val vehicleClass: VehicleClass,

        /**
         * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
         */
        val reservedBy: CustomerId,

        /**
         * @example 2024-11-02 16:59:01 Europe/Amsterdam
         */
        val occurredOn: LocalDateTime
    ): AnyReservationEvent

    data class VehicleCouldNotBeReserved(
        /**
         * @example "DE:M-CC-0001"
         */
        val vehicle: LicensePlate,

        /**
         * @example "fun vehicles"
         */
        val vehicleClass: VehicleClass,

        /**
         * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
         */
        val interestedCustomer: CustomerId,

        /**
         * @example "already reserved"
         */
        val reason: ReservationRejectionReason,

        /**
         * @example 2024-11-02 16:59:01 Europe/Amsterdam
         */
        val occurredOn: LocalDateTime
    ): AnyReservationEvent
}

sealed interface AnyReservationCommand: Command {
    data class PleaseReserveVehicle(
        /**
         * @example "DE:M-CC-0001"
         */
        val vehicle: LicensePlate,

        /**
         * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
         */
        val interestedCustomer: CustomerId,

        /**
         * @example 2024-11-02 16:59:01 Europe/Amsterdam
         */
        val issuedAt: LocalDateTime
    ): AnyReservationCommand
}
