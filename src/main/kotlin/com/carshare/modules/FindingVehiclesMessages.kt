package com.carshare.modules

import com.carshare.domain.VehicleClass
import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question

sealed interface AnyAvailableVehiclesEvent: Event {

}

sealed interface AnyAvailableVehiclesQuestion: Question {
}

sealed interface AnyAvailableVehiclesAnswer: Answer {

}

data class WhatVehiclesAreAvailableInTheArea(
    val fleet: String,
    val referenceLocation: String
) : Question, AnyAvailableVehiclesQuestion

data class AvailableVehicles(
    val fleet: String,
    val referenceLocation: String,
    val availableVehicles: List<AvailableVehicle>
): Answer, AnyAvailableVehiclesAnswer

data class AvailableVehicle(
    val vehicle: String,
    val location: String,
    val vehicleClass: VehicleClass
)

