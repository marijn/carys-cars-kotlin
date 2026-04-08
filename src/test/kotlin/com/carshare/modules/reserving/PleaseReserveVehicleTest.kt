package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

sealed class LicensePlate {
    data class DutchLicensePlate(val plateNo: String): LicensePlate();
    data class GermanLicensePlate(val plateNo: String): LicensePlate();
    data class SwissLicensePlate(val plateNo: String): LicensePlate();
    data class DanishLicensePlate(val plateNo: String): LicensePlate();
}

enum class VehicleClass {
    InAndAroundTheCity,
    FunVehicles,
    LongDistanceTrips,
    MovingBulkyThings,
}

public data class VehicleEnteredOperation(
    /**
     * @example "DE:M-CC-0001"
     */
    public val vehicle: LicensePlate,
    /**
     * @example "fun vehicles"
     */
    public val vehicleClass: VehicleClass,
    /**
     * @example 2024-11-02 16:59:01 Europe/Amsterdam
     */
    public val occurredOn: LocalDateTime
) : Event() {};

typealias CustomerId = String;

public data class VehicleWasReserved(
    /**
     * @example "DE:M-CC-0001"
     */
    public val vehicle: LicensePlate,

    /**
     * @example "fun vehicles"
     */
    public val vehicleClass: VehicleClass,

    /**
     * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
     */
    public val reservedBy: CustomerId,

    /**
     * @example 2024-11-02 16:59:01 Europe/Amsterdam
     */
    public val occurredOn: LocalDateTime
): Event() {};

sealed class ReservationRejectionReason {
    data class AlreadyReserved(
        private val currentlyReservedBy: String
    ): ReservationRejectionReason();
}

public data class VehicleCouldNotBeReserved(
    /**
     * @example "DE:M-CC-0001"
     */
    public val vehicle: LicensePlate,

    /**
     * @example "fun vehicles"
     */
    public val vehicleClass: VehicleClass,

    /**
     * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
     */
    public val interestedCustomer: CustomerId,

    /**
     * @example "already reserved"
     */
    public val reason: ReservationRejectionReason,

    /**
     * @example 2024-11-02 16:59:01 Europe/Amsterdam
     */
    public val occurredOn: LocalDateTime
): Event() {};

public data class PleaseReserveVehicle(
    /**
     * @example "DE:M-CC-0001"
     */
    public val vehicle: LicensePlate,

    /**
     * @example "customer:dae3ca24-b1e6-4f0e-85cb-0c4b9f5fab8b"
     */
    public val interestedCustomer: CustomerId,

    /**
     * @example 2024-11-02 16:59:01 Europe/Amsterdam
     */
    public val issuedAt: LocalDateTime
): Command() {};

sealed class ReservingState: State<ReservingState>() {
    class VehicleIsUnavailable(): ReservingState() {
        override fun evolve(event: Event): ReservingState {
            return when(event) {
                is VehicleEnteredOperation -> VehicleIsAvailable(event.vehicleClass)
                else -> this
            };
        }
    };

    class VehicleIsAvailable(
        internal val vehicleClass: VehicleClass
    ): ReservingState() {
        override fun evolve(event: Event): ReservingState {
            return when(event) {
                is VehicleWasReserved -> VehicleIsReserved(
                    this.vehicleClass,
                    event.reservedBy
                )
                else -> this
            };
        }
    };

    class VehicleIsReserved(
        internal val vehicleClass: VehicleClass,
        internal val reservedBy: CustomerId,
    ): ReservingState() {
        override fun evolve(event: Event): ReservingState {
            return this;
        }
    };
}

class ReservationDecider(state: ReservingState): Decider<ReservingState>(state) {
    override fun decide(command: Command): List<Event> {
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
                else -> listOf();
            }
            else -> listOf();
        }
    }
}

class PleaseReserveVehicleTest {
    @Test()
    fun `Is available`() {
        val scenario = CommandHandlingScenario()
            .given(
                VehicleEnteredOperation(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    LocalDateTime.parse("2024-11-02T20:19:53"),
                ),
            )
            .whenInstructed(
                PleaseReserveVehicle(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.parse("2024-11-02T20:19:54"),
                )
            )
            .thenExpect(
                VehicleWasReserved(
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
        val scenario = CommandHandlingScenario()
            .given(
                VehicleEnteredOperation(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    LocalDateTime.parse("2024-11-02T20:19:53"),
                ),
                VehicleWasReserved(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    VehicleClass.LongDistanceTrips,
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.parse("2024-11-02T20:19:54"),
                )
            )
            .whenInstructed(
                PleaseReserveVehicle(
                    LicensePlate.DutchLicensePlate("GHC-12-A"),
                    "customer:22222222-2222-2222-2222-222222222222",
                    LocalDateTime.parse("2024-11-02T20:19:55"),
                )
            )
            .thenExpect(
                VehicleCouldNotBeReserved(
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
