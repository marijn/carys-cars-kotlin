package com.carshare.infrastructure.decider.testing

import com.carshare.infrastructure.decider.Decider
import com.carshare.infrastructure.decider.State
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import org.assertj.core.api.Assertions

class CommandHandlingScenarioThenStep {
    private val givenEvents: List<Event>
    private val whenCommand: Command
    private val thenEvents: List<Event>

    /**
     * @internal
     * @see CommandHandlingScenario.given
     * @see CommandHandlingScenario.whenInstructed
     */
    constructor(
        givenEvents: List<Event>,
        whenCommand: Command,
        thenEvents: List<Event>
    ) {
        this.givenEvents = givenEvents;
        this.whenCommand = whenCommand;
        this.thenEvents = thenEvents;
    }

    fun <AnyState: State<AnyState>>assertOn(initialState: AnyState, subjectUnderTestFactory: (state: AnyState) -> Decider<AnyState>) {
        // arrange
        var currentState: AnyState = initialState;
        for (givenEvent in givenEvents) {
            currentState = currentState.evolve(givenEvent) as AnyState;
        }

        // act
        val subjectUnderTest = subjectUnderTestFactory(currentState);
        val actualEvents: List<Event> = subjectUnderTest.decide(whenCommand);

        // assert
        val expectedEvents = thenEvents
        Assertions.assertThat(actualEvents).usingRecursiveComparison().isEqualTo(expectedEvents)
    }
}

class CommandHandlingScenarioWhenStep {
    private val givenEvents: List<Event>
    private val whenCommand: Command

    /**
     * @internal
     * @see CommandHandlingScenario.given
     * @see CommandHandlingScenario.whenInstructed
     */
    constructor(
        givenEvents: List<Event>,
        whenCommand: Command
    ) {
        this.givenEvents = givenEvents;
        this.whenCommand = whenCommand;
    }

    fun thenExpect(
        vararg expectedEvents: Event
    ): CommandHandlingScenarioThenStep {
        return CommandHandlingScenarioThenStep(
                this.givenEvents,
                this.whenCommand,
                expectedEvents.toList()
        );
    }

    fun thenNothingShouldHaveHappened(): CommandHandlingScenarioThenStep {
        return CommandHandlingScenarioThenStep(
            this.givenEvents,
            this.whenCommand,
            listOf()
        );
    }
}

class CommandHandlingScenarioGivenStep {
    private val givenEvents: List<Event>

    /**
     * @internal
     * @see CommandHandlingScenario.given
     */
    constructor(givenEvents: List<Event>) {
        this.givenEvents = givenEvents;
    }

    fun whenInstructed(
        triggeringCommand: Command
    ): CommandHandlingScenarioWhenStep {
        return CommandHandlingScenarioWhenStep(
            this.givenEvents,
            triggeringCommand
        );
    }
}

class CommandHandlingScenario {
    fun given(
        vararg preConditions: Event
    ): CommandHandlingScenarioGivenStep {
        return CommandHandlingScenarioGivenStep(preConditions.toList());
    }

    fun whenInstructed(
        triggeringCommand: Command
    ): CommandHandlingScenarioWhenStep {
        val noPreviouslyHappenedEvents: List<Event> = listOf();

        return CommandHandlingScenarioGivenStep(noPreviouslyHappenedEvents)
            .whenInstructed(triggeringCommand);
    }
}