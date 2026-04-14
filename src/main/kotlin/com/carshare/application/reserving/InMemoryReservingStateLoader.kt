package com.carshare.application.reserving

import com.carshare.infrastructure.decider.StateLoader
import com.carshare.modules.LicensePlate
import com.carshare.modules.AnyReservationEvent
import com.carshare.modules.reserving.ReservingState

class InMemoryReservingStateLoader: StateLoader<LicensePlate, ReservingState, AnyReservationEvent> {
    private val storage: MutableMap<LicensePlate, ReservingState> = mutableMapOf();

    override fun add(identifiedBy: LicensePlate, state: ReservingState) {
        storage[identifiedBy] = state;
    }

    override fun loadStateOrElse(identifiedBy: LicensePlate, fallback: ReservingState): ReservingState {
        return storage.getOrElse(identifiedBy, { fallback });
    }
}