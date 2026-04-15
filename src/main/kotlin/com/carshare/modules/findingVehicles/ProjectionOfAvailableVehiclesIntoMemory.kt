package com.carshare.modules.findingVehicles

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import com.carshare.modules.fleetManagement.CarWasAddedToFleet
import com.carshare.modules.fleetManagement.CarWasRemovedFromFleet

class ProjectionOfAvailableVehiclesIntoMemory: Projection<Event, Question, Answer> {
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
            is CarWasRemovedFromFleet -> vehicles = vehicles.dropWhile { vehicle -> vehicle.vehicle == event.vehicle }
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