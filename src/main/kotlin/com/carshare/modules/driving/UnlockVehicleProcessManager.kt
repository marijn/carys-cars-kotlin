package com.carshare.modules.driving

import com.carshare.infrastructure.automation.ProcessManager
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import com.carshare.modules.UnlockVehicle
import com.carshare.modules.rentals.RentalStarted

class UnlockVehicleProcessManager: ProcessManager<Event, Command> {
    override fun processEvent(trigger: Event): List<Command> {
        return when(trigger) {
            is RentalStarted -> unlockVehicle(trigger.vehicle)
            else -> listOf()
        }
    }

    private fun unlockVehicle(vehicle: String): List<UnlockVehicle> = listOf(
        UnlockVehicle(
            vehicle,
        )
    )
}