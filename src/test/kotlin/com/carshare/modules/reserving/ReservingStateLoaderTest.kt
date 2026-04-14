package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Event
import com.carshare.modules.LicensePlate
import com.carshare.modules.VehicleClass
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class InMemoryReservingStateLoaderTest: ReservingStateLoaderTest() {
    override fun createSubjectUnderTest(): StateLoader<LicensePlate, ReservingState, AnyReservationEvent> {
        return InMemoryReservingStateLoader()
    }
}

abstract class ReservingStateLoaderTest {
    @Test()
    fun `Falls back to provided default state in absence of state`() {
        // arrange
        val stateLoader = createSubjectUnderTest();

        // act
        val fallback = ReservingState.VehicleIsUnavailable()
        val actual = stateLoader.loadStateOrElse(
            LicensePlate.DutchLicensePlate("JTG-59-R"),
            fallback
        );

        // assert
        val expected = fallback;
        Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test()
    fun `Returns state that was added before`() {
        // arrange
        val stateLoader = createSubjectUnderTest();
        val stateId = LicensePlate.DutchLicensePlate("JTG-59-R")
        val state = ReservingState.VehicleIsReserved(
            VehicleClass.FunVehicles,
            "customer:11111111-1111-1111-1111-111111111111"
        )
        stateLoader.add(stateId, state);

        // act
        val actual = stateLoader.loadStateOrElse(stateId, ReservingState.VehicleIsUnavailable());

        // assert
        val expected = state;
        Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    abstract fun createSubjectUnderTest(): StateLoader<LicensePlate, ReservingState, AnyReservationEvent>
}

interface StateLoader<AnyStateId, AnyState: State<AnyEvent>, AnyEvent: Event> {
    fun add(identifiedBy: AnyStateId, state: AnyState)

    fun loadStateOrElse(identifiedBy: AnyStateId, fallback: AnyState): AnyState
}

class InMemoryReservingStateLoader: StateLoader<LicensePlate, ReservingState, AnyReservationEvent> {
    private val storage: MutableMap<LicensePlate, ReservingState> = mutableMapOf();

    override fun add(identifiedBy: LicensePlate, state: ReservingState) {
        storage[identifiedBy] = state;
    }

    override fun loadStateOrElse(identifiedBy: LicensePlate, fallback: ReservingState): ReservingState {
        return storage.getOrElse(identifiedBy, { fallback });
    }
}