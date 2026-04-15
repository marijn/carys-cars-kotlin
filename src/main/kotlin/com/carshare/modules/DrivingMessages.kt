package com.carshare.modules

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

sealed interface AnyUnlockVehicleAutomationEvent: Event {
}

sealed interface AnyUnlockVehicleAutomationCommand: Command {
}

data class UnlockVehicle(
    /**
     * @example "NL:GGS-10-N"
     */
    val vehicle: String
): Command, AnyUnlockVehicleAutomationCommand