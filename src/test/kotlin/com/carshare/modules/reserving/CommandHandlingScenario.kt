package com.carshare.modules.reserving

import com.carshare.infrastructure.decider.Decider
import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import org.assertj.core.api.Assertions

class CommandHandlingScenarioThenStep<AnyCommand: Command, AnyEvent: Event> {
    private val givenEvents: List<AnyEvent>
    private val whenCommand: AnyCommand
    private val thenEvents: List<AnyEvent>

    /**
     * @internal
     * @see CommandHandlingScenario.given
     * @see CommandHandlingScenario.whenInstructed
     */
    constructor(
        givenEvents: List<AnyEvent>,
        whenCommand: AnyCommand,
        thenEvents: List<AnyEvent>
    ) {
        this.givenEvents = givenEvents;
        this.whenCommand = whenCommand;
        this.thenEvents = thenEvents;
    }

    fun <AnyState: State<AnyEvent>>assertOn(initialState: State<AnyEvent>, subjectUnderTestFactory: (state: AnyState) -> Decider<AnyCommand, AnyEvent, AnyState>) {
        // arrange
        var currentState: State<AnyEvent> = initialState;
        for (givenEvent in givenEvents) {
            currentState = currentState.evolve(givenEvent);
        }

        // act
        val subjectUnderTest = subjectUnderTestFactory(currentState as AnyState);
        val actualEvents: List<AnyEvent> = subjectUnderTest.decide(whenCommand);

        // assert
        val expectedEvents = thenEvents
        Assertions.assertThat(actualEvents).usingRecursiveComparison().isEqualTo(expectedEvents)
    }
}

class CommandHandlingScenarioWhenStep<AnyCommand: Command, AnyEvent: Event> {
    private val givenEvents: List<AnyEvent>
    private val whenCommand: AnyCommand

    /**
     * @internal
     * @see CommandHandlingScenario.given
     * @see CommandHandlingScenario.whenInstructed
     */
    constructor(
        givenEvents: List<AnyEvent>,
        whenCommand: AnyCommand
    ) {
        this.givenEvents = givenEvents;
        this.whenCommand = whenCommand;
    }

    fun thenExpect(
        vararg expectedEvents: AnyEvent
    ): CommandHandlingScenarioThenStep<AnyCommand, AnyEvent> {
        return CommandHandlingScenarioThenStep(
                this.givenEvents,
                this.whenCommand,
                expectedEvents.toList()
        );
    }

    fun thenNothingShouldHaveHappened(): CommandHandlingScenarioThenStep<AnyCommand, AnyEvent> {
        return CommandHandlingScenarioThenStep(
            this.givenEvents,
            this.whenCommand,
            listOf()
        );
    }
}

class CommandHandlingScenarioGivenStep<AnyCommand: Command, AnyEvent: Event> {
    private val givenEvents: List<AnyEvent>

    /**
     * @internal
     * @see CommandHandlingScenario.given
     */
    constructor(givenEvents: List<AnyEvent>) {
        this.givenEvents = givenEvents;
    }

    fun whenInstructed(
        triggeringCommand: AnyCommand
    ): CommandHandlingScenarioWhenStep<AnyCommand, AnyEvent> {
        return CommandHandlingScenarioWhenStep(
            this.givenEvents,
            triggeringCommand
        );
    }
}

class CommandHandlingScenario<AnyCommand: Command, AnyEvent: Event> {
    fun given(
        vararg preConditions: AnyEvent
    ): CommandHandlingScenarioGivenStep<AnyCommand, AnyEvent> {
        return CommandHandlingScenarioGivenStep(preConditions.toList());
    }

    fun whenInstructed(
        triggeringCommand: AnyCommand
    ): CommandHandlingScenarioWhenStep<AnyCommand, AnyEvent> {
        val noPreviouslyHappenedEvents: List<AnyEvent> = listOf();

        return CommandHandlingScenarioGivenStep<AnyCommand, AnyEvent>(noPreviouslyHappenedEvents)
            .whenInstructed(triggeringCommand);
    }
}