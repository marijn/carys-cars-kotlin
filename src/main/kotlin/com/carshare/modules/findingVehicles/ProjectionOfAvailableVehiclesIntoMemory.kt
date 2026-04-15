package com.carshare.modules.findingVehicles

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import com.carshare.modules.AnyAvailableVehiclesAnswer
import com.carshare.modules.AnyAvailableVehiclesEvent
import com.carshare.modules.AnyAvailableVehiclesQuestion
import com.carshare.modules.AvailableVehicle
import com.carshare.modules.AvailableVehicles
import com.carshare.modules.WhatVehiclesAreAvailableInTheArea
import com.carshare.modules.CarWasAddedToFleet
import com.carshare.modules.CarWasRemovedFromFleet

class ProjectionOfAvailableVehiclesIntoMemory: Projection<AnyAvailableVehiclesEvent, AnyAvailableVehiclesQuestion, AnyAvailableVehiclesAnswer> {
    private var vehicles = listOf<AvailableVehicle>()

    override fun acknowledge(event: AnyAvailableVehiclesEvent) {
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

    override fun ask(question: AnyAvailableVehiclesQuestion): AnyAvailableVehiclesAnswer {
        return when(question) {
            is WhatVehiclesAreAvailableInTheArea -> AvailableVehicles(
                question.fleet,
                question.referenceLocation,
                vehicles
            )
        }
    }
}