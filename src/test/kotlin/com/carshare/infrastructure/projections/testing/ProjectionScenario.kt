package com.carshare.infrastructure.projections.testing

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import org.assertj.core.api.Assertions

class ProjectionScenarioThenStep<AnyEvent: Event, AnyQuestion: Question, AnyAnswer: Answer>(
    private val givenEvents: List<AnyEvent>,
    private val whenQuestion: AnyQuestion,
    private val thenAnswer: AnyAnswer
) {
    fun assertOnProjection(subjectUnderTest: Projection<AnyEvent, AnyQuestion, AnyAnswer>) {
        // arrange
        givenEvents.forEach { e -> subjectUnderTest.acknowledge(e) }

        // act
        val actualAnswer: Answer = subjectUnderTest.ask(whenQuestion)

        // assert
        Assertions.assertThat(actualAnswer).usingRecursiveComparison().isEqualTo(thenAnswer)
    }
}

class ProjectionScenarioWhenStep<AnyEvent: Event, AnyQuestion: Question, AnyAnswer: Answer>(
    private val givenEvents: List<AnyEvent>,
    private val whenQuestion: AnyQuestion
) {
    fun thenExpect(
        expectedAnswer: AnyAnswer
    ): ProjectionScenarioThenStep<AnyEvent, AnyQuestion, AnyAnswer> {
        return ProjectionScenarioThenStep<AnyEvent, AnyQuestion, AnyAnswer>(
            givenEvents,
            whenQuestion,
            expectedAnswer
        )
    }
}

class ProjectionScenarioGivenStep<AnyEvent: Event, AnyQuestion: Question, AnyAnswer: Answer>(
    private val givenEvents: List<AnyEvent>
) {
    fun whenAskedFor(
        question: AnyQuestion
    ): ProjectionScenarioWhenStep<AnyEvent, AnyQuestion, AnyAnswer> {
        return ProjectionScenarioWhenStep<AnyEvent, AnyQuestion, AnyAnswer>(
            givenEvents,
            question
        )
    }
}

class ProjectionScenario<AnyEvent: Event, AnyQuestion: Question, AnyAnswer: Answer> {
    fun given(
        vararg preConditions: AnyEvent
    ): ProjectionScenarioGivenStep<AnyEvent, AnyQuestion, AnyAnswer> {
        return ProjectionScenarioGivenStep<AnyEvent, AnyQuestion, AnyAnswer>(preConditions.toList())
    }

    fun whenAskedFor(
        question: AnyQuestion
    ): ProjectionScenarioWhenStep<AnyEvent, AnyQuestion, AnyAnswer> {
        val noPreviouslyHappenedEvents: List<AnyEvent> = listOf()

        return ProjectionScenarioGivenStep<AnyEvent, AnyQuestion, AnyAnswer>(noPreviouslyHappenedEvents)
            .whenAskedFor(question)
    }
}
