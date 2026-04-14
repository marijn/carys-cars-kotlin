package com.carshare.infrastructure.decider

import com.carshare.infrastructure.messaging.Event

interface StateLoader<AnyStateId, AnyState: State<AnyEvent>, AnyEvent: Event> {
    // FIXME: suspend
    fun add(identifiedBy: AnyStateId, state: AnyState)

    // FIXME: suspend
    fun loadStateOrElse(identifiedBy: AnyStateId, fallback: AnyState): AnyState
}