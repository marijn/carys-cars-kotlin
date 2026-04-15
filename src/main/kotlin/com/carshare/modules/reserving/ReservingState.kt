package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.State
import com.carshare.modules.AnyReservationEvent
import com.carshare.modules.CustomerId
import com.carshare.modules.VehicleClass
import com.carshare.modules.VehicleCouldNotBeReserved
import com.carshare.modules.VehicleEnteredOperation
import com.carshare.modules.VehicleWasReserved

sealed interface ReservingState: State<AnyReservationEvent> {
    class VehicleIsUnavailable: ReservingState {
        override fun evolve(event: AnyReservationEvent): ReservingState {
            return when(event) {
                is VehicleEnteredOperation -> VehicleIsAvailable(event.vehicleClass)
                is VehicleCouldNotBeReserved -> this
                is VehicleWasReserved -> this
            };
        }
    };

    class VehicleIsAvailable(
        internal val vehicleClass: VehicleClass
    ): ReservingState {
        override fun evolve(event: AnyReservationEvent): ReservingState {
            return when(event) {
                is VehicleWasReserved -> VehicleIsReserved(
                    this.vehicleClass,
                    event.reservedBy
                )
                is VehicleCouldNotBeReserved -> this
                is VehicleEnteredOperation -> this
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