package com.carshare.infrastructure.projection

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question

interface Projection<AnyEvent, AnyQuestion, AnyAnswer> {
    fun acknowledge(event: AnyEvent);

    fun ask(question: AnyQuestion): AnyAnswer
}
