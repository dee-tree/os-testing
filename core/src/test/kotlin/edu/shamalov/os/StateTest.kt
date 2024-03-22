package edu.shamalov.os

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertIs

class StateTest {

    @ParameterizedTest
    @MethodSource("taskTypesAndEvents")
    fun testRunning(isBasic: Boolean, event: Event) {
        val state = State.Running(isBasic)

        when {
            event is Event.Terminate -> assertIs<State.Suspended>(state.succeededBy(event))
            event is Event.Preempt -> assertIs<State.Ready>(state.succeededBy(event))
            event is Event.Wait && !isBasic -> assertIs<ExtendedState.Waiting>(state.succeededBy(event))
            else -> assertThrows { state.succeededBy(event) }
        }
    }

    // TODO: Add the same tests as `testRunning` for all remaining states

    companion object {
        val events = arrayOf(
            Event.Activate, Event.Start, Event.Preempt, Event.Terminate,
            Event.Wait, Event.Release
        )

        @JvmStatic
        fun taskTypesAndEvents(): List<Arguments> = booleanArrayOf(true, false).map { isBasicTask ->
            events.map { event -> Arguments { arrayOf(isBasicTask, event) } }
        }.flatten()
    }
}