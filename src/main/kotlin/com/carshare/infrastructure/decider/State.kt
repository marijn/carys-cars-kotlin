package com.carshare.infrastructure.decider

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event

abstract class State<AnyState> {
    abstract fun evolve(event: Event): State<AnyState>
}

abstract class Decider<AnyState: State<AnyState>>(public val state: AnyState) {
    abstract fun decide(command: Command): List<Event>
}