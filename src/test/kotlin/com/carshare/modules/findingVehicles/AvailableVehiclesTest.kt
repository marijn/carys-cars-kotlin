package com.carshare.modules.findingVehicles

import com.carshare.domain.VehicleClass
import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import com.carshare.infrastructure.projection.testing.ProjectionScenario
import org.junit.jupiter.api.Test

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
    override fun acknowledge(event: Event) {
    }

    override fun ask(question: Question): Answer {
        return when(question) {
            is WhatVehiclesAreAvailableInTheArea -> AvailableVehicles(
                question.fleet,
                question.referenceLocation,
                listOf()
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
}
