package com.carshare.infrastructure.automation

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

interface ProcessManager {
    fun processEvent(trigger: Event): List<Command>
}