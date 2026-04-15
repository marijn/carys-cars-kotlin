package com.carshare.infrastructure.automation.testing

import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.automation.ProcessManager
import org.assertj.core.api.Assertions

class AutomationScenarioThenStep<AnyEvent: Event, AnyCommand: Command>(
    private val givenEvents: List<AnyEvent>,
    private val whenEvent: AnyEvent,
    private val thenCommands: List<AnyCommand>
) {
    fun assertOnProcessManager(subjectUnderTest: ProcessManager<AnyEvent, AnyCommand>) {
        // arrange
        givenEvents.forEach { e -> subjectUnderTest.processEvent(e) }

        // act
        val actualCommands: List<AnyCommand> = subjectUnderTest.processEvent(whenEvent)

        // assert
        Assertions.assertThat(actualCommands).usingRecursiveComparison().isEqualTo(thenCommands)
    }
}

class AutomationScenarioWhenStep<AnyEvent: Event, AnyCommand: Command>(
    private val givenEvents: List<AnyEvent>,
    private val whenEvent: AnyEvent
) {
    fun thenExpect(
        vararg expectedCommands: AnyCommand
    ): AutomationScenarioThenStep<AnyEvent, AnyCommand> {
        return AutomationScenarioThenStep<AnyEvent, AnyCommand>(
            givenEvents,
            whenEvent,
            expectedCommands.toList()
        )
    }

    fun thenNothingShouldHaveHappened(): AutomationScenarioThenStep<AnyEvent, AnyCommand> {
        return AutomationScenarioThenStep<AnyEvent, AnyCommand>(
            givenEvents,
            whenEvent,
            listOf()
        )
    }
}

class AutomationScenarioGivenStep<AnyEvent: Event, AnyCommand: Command>(
    private val givenEvents: List<AnyEvent>
) {
    fun whenTriggeredBecauseOf(
        triggeringEvent: AnyEvent
    ): AutomationScenarioWhenStep<AnyEvent, AnyCommand> {
        return AutomationScenarioWhenStep<AnyEvent, AnyCommand>(
            givenEvents,
            triggeringEvent
        )
    }
}

class AutomationScenario<AnyEvent: Event, AnyCommand: Command> {
    fun given(
        vararg preConditions: AnyEvent
    ): AutomationScenarioGivenStep<AnyEvent, AnyCommand> {
        return AutomationScenarioGivenStep<AnyEvent, AnyCommand>(preConditions.toList())
    }

    fun whenTriggeredBecauseOf(
        triggeringEvent: AnyEvent
    ): AutomationScenarioWhenStep<AnyEvent, AnyCommand> {
        val noPreviouslyHappenedEvents: List<AnyEvent> = listOf()

        return AutomationScenarioGivenStep<AnyEvent, AnyCommand>(noPreviouslyHappenedEvents)
            .whenTriggeredBecauseOf(triggeringEvent)
    }
}