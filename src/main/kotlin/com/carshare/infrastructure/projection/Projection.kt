package com.carshare.infrastructure.projection

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question

interface Projection {
    fun acknowledge(event: Event)

    fun ask(question: Question): Answer
}