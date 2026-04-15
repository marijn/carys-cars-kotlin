package com.carshare.infrastructure.automation

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

interface ProcessManager<AnyEvent: Event, AnyCommand: Command> {
    fun processEvent(trigger: AnyEvent): List<AnyCommand>
}
