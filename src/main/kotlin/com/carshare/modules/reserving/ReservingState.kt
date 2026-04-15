package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.State
import com.carshare.modules.AnyReservationEvent
import com.carshare.modules.CustomerId
import com.carshare.modules.VehicleClass

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