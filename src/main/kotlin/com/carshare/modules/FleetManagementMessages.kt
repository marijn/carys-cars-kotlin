package com.carshare.modules

import com.carshare.domain.VehicleClass
import com.carshare.infrastructure.messaging.Event
import java.time.LocalDateTime

data class CarWasAddedToFleet (
    val vehicle: String,
    val parkedLocation: String,
    val odometer: Double,
    val vehicleClass: VehicleClass,
    val addedToFleetAt: LocalDateTime
): Event

data class CarWasRemovedFromFleet (
    val vehicle: String,
    val odometer: Double,
    val removedFromFleetAt: LocalDateTime
): Event