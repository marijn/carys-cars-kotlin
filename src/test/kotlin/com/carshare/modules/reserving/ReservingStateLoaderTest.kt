package com.carshare.modules.reserving

import com.carshare.application.reserving.InMemoryReservingStateLoader
import com.carshare.infrastructure.decider.StateLoader
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

