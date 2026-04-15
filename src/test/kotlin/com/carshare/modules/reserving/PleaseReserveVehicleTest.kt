package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.testing.CommandHandlingScenario
import com.carshare.modules.AnyReservationCommand
import com.carshare.modules.AnyReservationEvent
import com.carshare.modules.LicensePlate
import com.carshare.modules.VehicleClass
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class PleaseReserveVehicleTest {
    @Test()
    fun `Is available`() {
        val scenario = CommandHandlingScenario<AnyReservationCommand, AnyReservationEvent>()
            .given(
                AnyReservationEvent.VehicleEnteredOperation(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    LocalDateTime.parse("2024-11-02T20:19:53"),
                ),
            )
            .whenInstructed(
                AnyReservationCommand.PleaseReserveVehicle(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.parse("2024-11-02T20:19:54"),
                )
            )
            .thenExpect(
                AnyReservationEvent.VehicleWasReserved(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.parse("2024-11-02T20:19:54"),
                )
            )
            .assertOn(ReservingState.VehicleIsUnavailable(), {
                    state: ReservingState ->
                ReservingDecider(state)
            })
    }

    @Test()
    fun `Is reserved`() {
        val scenario = CommandHandlingScenario<AnyReservationCommand, AnyReservationEvent>()
            .given(
                AnyReservationEvent.VehicleEnteredOperation(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    LocalDateTime.parse("2024-11-02T20:19:53"),
                ),
                AnyReservationEvent.VehicleWasReserved(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.parse("2024-11-02T20:19:54"),
                )
            )
            .whenInstructed(
                AnyReservationCommand.PleaseReserveVehicle(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    "customer:22222222-2222-2222-2222-222222222222",
                    LocalDateTime.parse("2024-11-02T20:19:55"),
                )
            )
            .thenExpect(
                AnyReservationEvent.VehicleCouldNotBeReserved(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    "customer:22222222-2222-2222-2222-222222222222",
                    ReservationRejectionReason.AlreadyReserved("customer:11111111-1111-1111-1111-111111111111"),
                    LocalDateTime.parse("2024-11-02T20:19:55"),
                )
            )
            .assertOn(ReservingState.VehicleIsUnavailable(), {
                    state: ReservingState ->
                ReservingDecider(state)
            })
    }
}
