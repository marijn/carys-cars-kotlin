package com.carshare.modules.findingVehicles

import com.carshare.domain.VehicleClass
import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import com.carshare.infrastructure.projection.testing.ProjectionScenario
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

data class CarWasAddedToFleet (
    val vehicle: String,
    val parkedLocation: String,
    val odometer: Double,
    val vehicleClass: VehicleClass,
    val addedToFleetAt: LocalDateTime
): Event()

data class WhatVehiclesAreAvailableInTheArea(
    val fleet: String,
    val referenceLocation: String
) : Question()

data class AvailableVehicles(
    val fleet: String,
    val referenceLocation: String,
    val availableVehicles: List<AvailableVehicle>
): Answer()

data class AvailableVehicle(
    val vehicle: String,
    val location: String,
    val vehicleClass: VehicleClass
)

class AvailableVehiclesInMemoryProjection: Projection {
    private var vehicles = listOf<AvailableVehicle>()

    override fun acknowledge(event: Event) {
        when (event) {
            is CarWasAddedToFleet -> vehicles = vehicles.plus(
                AvailableVehicle(
                    event.vehicle,
                    event.parkedLocation,
                    event.vehicleClass
                )
            )
        }
    }

    override fun ask(question: Question): Answer {
        return when(question) {
            is WhatVehiclesAreAvailableInTheArea -> AvailableVehicles(
                question.fleet,
                question.referenceLocation,
                vehicles
            )
            else -> throw Exception("Unknown query")
        }
    }
}

class AvailableVehiclesTest {

    @Test
    fun `It answers with no availability when no cars have been added to the fleet` () {
        val scenario = ProjectionScenario()
            .whenAskedFor(
                WhatVehiclesAreAvailableInTheArea(
                    "NL",
                    "52,37117° N, 4,87950° E"
                )
            )
            .thenExpect(
                AvailableVehicles(
                    "NL",
                    "52,37117° N, 4,87950° E",
                    listOf()
                )
            )
            .assertOnProjection(AvailableVehiclesInMemoryProjection())
    }
    @Test
    fun `It answers with availability when cars have been added to the fleet which have not been rented out` () {
        val scenario = ProjectionScenario()
            .given(
                CarWasAddedToFleet(
                    "NL:HTZ-11-G",
                    "52,34226° N, 4,87345° E",
                    23.3,
                    VehicleClass.FUN_PREMIUM,
                    LocalDateTime.parse("2025-11-12T09:03:01")
                ),
                CarWasAddedToFleet(
                    "NL:HZL-55-X",
                    "52,34508° N, 4,84512° E",
                    27.1,
                    VehicleClass.FUN_PREMIUM,
                    LocalDateTime.parse("2025-11-12T10:15:04")
                )
            )
            .whenAskedFor(
                WhatVehiclesAreAvailableInTheArea(
                    "NL",
                    "52,37117° N, 4,87950° E"
                )
            )
            .thenExpect(
                AvailableVehicles(
                    "NL",
                    "52,37117° N, 4,87950° E",
                    listOf(
                        AvailableVehicle(
                            "NL:HTZ-11-G",
                            "52,34226° N, 4,87345° E",
                            VehicleClass.FUN_PREMIUM
                        ),
                        AvailableVehicle(
                            "NL:HZL-55-X",
                            "52,34508° N, 4,84512° E",
                            VehicleClass.FUN_PREMIUM
                        ),
                    )
                )
            )
            .assertOnProjection(AvailableVehiclesInMemoryProjection())
    }
}
