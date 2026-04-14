package com.carshare.infrastructure.decider

import com.carshare.infrastructure.messaging.Event

interface StateLoader<AnyStateId, AnyState: State<AnyEvent>, AnyEvent: Event> {
    fun add(identifiedBy: AnyStateId, state: AnyState)

    fun loadStateOrElse(identifiedBy: AnyStateId, fallback: AnyState): AnyState
}