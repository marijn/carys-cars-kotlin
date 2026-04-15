package com.carshare.modules.findingVehicles

import com.carshare.domain.VehicleClass
import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Question

data class WhatVehiclesAreAvailableInTheArea(
    val fleet: String,
    val referenceLocation: String
) : Question

data class AvailableVehicles(
    val fleet: String,
    val referenceLocation: String,
    val availableVehicles: List<AvailableVehicle>
): Answer

data class AvailableVehicle(
    val vehicle: String,
    val location: String,
    val vehicleClass: VehicleClass
)