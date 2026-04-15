package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.modules.AnyReservationCommand
import com.carshare.modules.AnyReservationEvent

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