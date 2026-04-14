package com.carshare.modules

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

sealed interface TrialAutomationEvent: Event {

}

sealed interface TrialAutomationCommand: Command {

}