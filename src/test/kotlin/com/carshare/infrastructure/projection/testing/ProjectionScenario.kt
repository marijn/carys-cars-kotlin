package com.carshare.infrastructure.projection.testing

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question
import com.carshare.infrastructure.projection.Projection
import org.assertj.core.api.Assertions

class ProjectionScenarioThenStep(
    private val givenEvents: List<Event>,
    private val whenQuestion: Question,
    private val thenAnswer: Answer
) {
    fun assertOnProjection(subjectUnderTest: Projection) {
        // arrange
        givenEvents.forEach { e -> subjectUnderTest.acknowledge(e) }

        // act
        val actualAnswer: Answer = subjectUnderTest.ask(whenQuestion)

        // assert
        Assertions.assertThat(actualAnswer).usingRecursiveComparison().isEqualTo(thenAnswer)
    }
}

class ProjectionScenarioWhenStep(
    private val givenEvents: List<Event>,
    private val whenQuestion: Question
) {
    fun thenExpect(
        expectedAnswer: Answer
    ): ProjectionScenarioThenStep {
        return ProjectionScenarioThenStep(
            givenEvents,
            whenQuestion,
            expectedAnswer
        )
    }
}

class ProjectionScenarioGivenStep(
    private val givenEvents: List<Event>
) {
    fun whenAskedFor(
        question: Question
    ): ProjectionScenarioWhenStep {
        return ProjectionScenarioWhenStep(
            givenEvents,
            question
        )
    }
}

class ProjectionScenario {
    fun given(
        vararg preConditions: Event
    ): ProjectionScenarioGivenStep {
        return ProjectionScenarioGivenStep(preConditions.toList())
    }

    fun whenAskedFor(
        question: Question
    ): ProjectionScenarioWhenStep {
        val noPreviouslyHappenedEvents: List<Event> = listOf()

        return ProjectionScenarioGivenStep(noPreviouslyHappenedEvents)
            .whenAskedFor(question)
    }
}
