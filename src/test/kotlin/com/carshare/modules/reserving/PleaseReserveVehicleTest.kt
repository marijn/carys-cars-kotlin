package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import com.carshare.modules.LicensePlate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

enum class VehicleClass {
    InAndAroundTheCity,
    FunVehicles,
    LongDistanceTrips,
    MovingBulkyThings,
}

typealias CustomerId = String;

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

sealed class ReservationRejectionReason {
    data class AlreadyReserved(
        private val currentlyReservedBy: String
    ): ReservationRejectionReason();
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

class ReservationDecider(
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
                ReservationDecider(state)
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
                ReservationDecider(state)
            })
    }
}
