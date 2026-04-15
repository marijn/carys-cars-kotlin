package com.carshare.infrastructure.decider

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

interface State<AnyEvent: Event> {
    fun evolve(event: AnyEvent): State<AnyEvent>
}

interface Decider<AnyCommand: Command, AnyEvent: Event, AnyState: State<AnyEvent>> {
    val state: AnyState

    fun decide(command: AnyCommand): List<AnyEvent>
}
