package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.modules.AnyReservationCommand
import com.carshare.modules.AnyReservationEvent
import com.carshare.modules.PleaseReserveVehicle
import com.carshare.modules.VehicleCouldNotBeReserved
import com.carshare.modules.VehicleWasReserved

class ReservingDecider(
    override val state: ReservingState
): Decider<AnyReservationCommand, AnyReservationEvent, ReservingState> {
    override fun decide(command: AnyReservationCommand): List<AnyReservationEvent> {
        return when (command) {
            is PleaseReserveVehicle -> when(state) {
                is ReservingState.VehicleIsAvailable -> listOf(
                    VehicleWasReserved(
                        command.vehicle,
                        state.vehicleClass,
                        command.interestedCustomer,
                        command.issuedAt,
                    )
                )
                is ReservingState.VehicleIsReserved -> listOf(
                    VehicleCouldNotBeReserved(
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