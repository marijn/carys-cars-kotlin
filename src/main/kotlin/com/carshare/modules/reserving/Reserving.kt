package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import com.carshare.modules.CustomerId
import com.carshare.modules.LicensePlate
import com.carshare.modules.VehicleClass
import org.springframework.cglib.core.Local
import java.time.LocalDateTime
import java.time.ZonedDateTime

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

sealed interface ReservingState: State<AnyReservationEvent> {
    class VehicleIsUnavailable: ReservingState {
        override fun evolve(event: AnyReservationEvent): ReservingState {
            return when(event) {
                is AnyReservationEvent.VehicleEnteredOperation -> VehicleIsAvailable(event.vehicleClass)
                is AnyReservationEvent.VehicleCouldNotBeReserved -> this
                is AnyReservationEvent.VehicleWasReserved -> this
            };
        }
    };

    class VehicleIsAvailable(
        internal val vehicleClass: VehicleClass
    ): ReservingState {
        override fun evolve(event: AnyReservationEvent): ReservingState {
            return when(event) {
                is AnyReservationEvent.VehicleWasReserved -> VehicleIsReserved(
                    this.vehicleClass,
                    event.reservedBy
                )
                is AnyReservationEvent.VehicleCouldNotBeReserved -> this
                is AnyReservationEvent.VehicleEnteredOperation -> this
            };
        }
    };

    class VehicleIsReserved(
        internal val vehicleClass: VehicleClass,
        internal val reservedBy: CustomerId,
    ): ReservingState {
        override fun evolve(event: AnyReservationEvent): ReservingState {
            return this;
        }
    };
}

class ReservingDecider(
    override val state: ReservingState
): Decider<AnyReservationCommand, AnyReservationEvent, ReservingState> {
    override fun decide(command: AnyReservationCommand): List<AnyReservationEvent> {
        return when (command) {
            is AnyReservationCommand.PleaseReserveVehicle -> when(state) {
                is ReservingState.VehicleIsAvailable -> listOf(
                    AnyReservationEvent.VehicleWasReserved(
                        command.vehicle,
                        state.vehicleClass,
                        command.interestedCustomer,
                        command.issuedAt,
                    )
                )
                is ReservingState.VehicleIsReserved -> listOf(
                    AnyReservationEvent.VehicleCouldNotBeReserved(
                        command.vehicle,
                        state.vehicleClass,
                        command.interestedCustomer,
                        ReservationRejectionReason.AlreadyReserved(state.reservedBy),
                        command.issuedAt,
                    )
                )
                is ReservingState.VehicleIsUnavailable -> emptyList()
            }
        }
    }
}

sealed class ReservationRejectionReason {
    data class AlreadyReserved(
        private val currentlyReservedBy: String
    ): ReservationRejectionReason();
}