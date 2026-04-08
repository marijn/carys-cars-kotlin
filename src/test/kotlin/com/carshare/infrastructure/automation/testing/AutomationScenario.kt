package com.carshare.infrastructure.automation.testing

import com.carshare.infrastructure.automation.ProcessManager
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import org.assertj.core.api.Assertions

class AutomationScenarioThenStep(
    private val givenEvents: List<Event>,
    private val whenEvent: Event,
    private val thenCommands: List<Command>
) {
    fun assertOnProcessManager(subjectUnderTest: ProcessManager) {
        // arrange
        givenEvents.forEach { e -> subjectUnderTest.processEvent(e) }

        // act
        val actualCommands: List<Command> = subjectUnderTest.processEvent(whenEvent)

        // assert
        Assertions.assertThat(actualCommands).usingRecursiveComparison().isEqualTo(thenCommands)
    }
}

class AutomationScenarioWhenStep(
    private val givenEvents: List<Event>,
    private val whenEvent: Event
) {
    fun thenExpect(
        vararg expectedCommands: Command
    ): AutomationScenarioThenStep {
        return AutomationScenarioThenStep(
            givenEvents,
            whenEvent,
            expectedCommands.toList()
        )
    }

    fun thenNothingShouldHaveHappened(): AutomationScenarioThenStep {
        return AutomationScenarioThenStep(
            givenEvents,
            whenEvent,
            listOf()
        )
    }
}

class AutomationScenarioGivenStep(
    private val givenEvents: List<Event>
) {
    fun whenTriggeredBecauseOf(
        triggeringEvent: Event
    ): AutomationScenarioWhenStep {
        return AutomationScenarioWhenStep(
            givenEvents,
            triggeringEvent
        )
    }
}

class AutomationScenario {
    fun given(
        vararg preConditions: Event
    ): AutomationScenarioGivenStep {
        return AutomationScenarioGivenStep(preConditions.toList())
    }

    fun whenTriggeredBecauseOf(
        triggeringEvent: Event
    ): AutomationScenarioWhenStep {
        val noPreviouslyHappenedEvents: List<Event> = listOf()

        return AutomationScenarioGivenStep(noPreviouslyHappenedEvents)
            .whenTriggeredBecauseOf(triggeringEvent)
    }
}
