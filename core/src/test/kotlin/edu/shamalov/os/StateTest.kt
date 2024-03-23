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
        val runningState = State.Running(isBasic)

        when {
            event is Event.Terminate -> assertIs<State.Suspended>(runningState.succeededBy(event))
            event is Event.Preempt -> assertIs<State.Ready>(runningState.succeededBy(event))
            event is Event.Wait && !isBasic -> assertIs<ExtendedState.Waiting>(runningState.succeededBy(event))
            else -> assertThrows { runningState.succeededBy(event) }
        }
    }

    @ParameterizedTest
    @MethodSource("taskTypesAndEvents")
    fun testSuspended(isBasic: Boolean, event: Event) {
        val suspendedState = State.Suspended(isBasic)

        when (event) {
            is Event.Activate -> assertIs<State.Ready>(suspendedState.succeededBy(event))
            else -> assertThrows<Throwable> { suspendedState.succeededBy(event) }
        }
    }

    @ParameterizedTest
    @MethodSource("taskTypesAndEvents")
    fun testReady(isBasic: Boolean, event: Event) {
        val readyState = State.Ready(isBasic)

        when (event) {
            is Event.Start -> assertIs<State.Running>(readyState.succeededBy(event))
            else -> assertThrows<Throwable> { readyState.succeededBy(event) }
        }
    }

    @ParameterizedTest
    @MethodSource("taskTypesAndEvents")
    fun testWaiting(isBasic: Boolean, event: Event) {
        val waitingState = ExtendedState.Waiting

        when (event) {
            is Event.Release -> assertIs<State.Ready>(waitingState.succeededBy(event))
            else -> assertThrows<Throwable> { waitingState.succeededBy(event) }
        }
    }

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