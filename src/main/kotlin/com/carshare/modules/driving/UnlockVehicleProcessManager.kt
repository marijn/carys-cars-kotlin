package com.carshare.modules.driving

import com.carshare.infrastructure.automation.ProcessManager
import com.carshare.infrastructure.messaging.Command
import com.carshare.modules.AnyUnlockVehicleAutomationEvent
import com.carshare.modules.UnlockVehicle
import com.carshare.modules.RentalStarted

class UnlockVehicleProcessManager: ProcessManager<AnyUnlockVehicleAutomationEvent, Command> {
    override fun processEvent(trigger: AnyUnlockVehicleAutomationEvent): List<Command> {
        return when(trigger) {
            is RentalStarted -> unlockVehicle(trigger.vehicle)
        }
    }

    private fun unlockVehicle(vehicle: String): List<UnlockVehicle> = listOf(
        UnlockVehicle(
            vehicle,
        )
    )
}