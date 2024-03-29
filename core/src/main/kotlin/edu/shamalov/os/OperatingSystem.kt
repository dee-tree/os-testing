package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext

abstract class OperatingSystem(private val processorCores: Int = DEFAULT_PROCESSOR_CORES) : AutoCloseable {
    protected abstract val scheduler: Scheduler
    private var isActive = true


    private val processorDispatcher = newFixedThreadPoolContext(processorCores, "processor")
    private val osDispatcher = newSingleThreadContext("OS")

    private lateinit var task: Task

    suspend fun start() = coroutineScope {
        launch(Dispatchers.Default) {
            logger.debug { "OS is started | Waiting for a task to execute" }

            launch(processorDispatcher) {
                task = scheduler.pop().await()
                logger.debug { "Processor received task to execute $task" }

                while (isActive && processorDispatcher.isActive) {
                    launch(Dispatchers.IO) { // higher priority task check
                        delay(10)
                        while (task.state is State.Running) {
                            if (scheduler.hasHigherPriorityTask(task.priority)) {
                                scheduler.lock(this@OperatingSystem)
                                require(task.onEvent(Event.Preempt) is State.Ready)
                                break
                            }
                        }
                    }
                    val state = try {
                        task.onEvent(Event.Start)
                    } catch (e: CancellationException) {
                        require(task.state is State.Ready) // after preempt
                        val higherPriorityTask = scheduler.pop().await()
                        scheduler.offer(task)
                        logger.debug { "Swap running task $task with a higher priority one: $higherPriorityTask" }
                        task = higherPriorityTask
                        scheduler.unlock(this@OperatingSystem)
                        continue
                    }

                    when (state) {
                        is State.Suspended -> {
                            logger.debug { "$task finished execution" }
                            task = scheduler.pop().await()
                        }

                        is ExtendedState.Waiting -> {
                            logger.debug { "$task is waiting for an event" }
                            launch(Dispatchers.IO) {
                                require(task.onEvent(Event.Release) is State.Ready)
                                scheduler.offer(task)
                            }
                            task = scheduler.pop().await()
                        }

                        else -> throw IllegalStateException("$task")
                    }
                }
            }
        }
    }

    override fun close() {
        if (!isActive) return
        isActive = false
        processorDispatcher.close()
        osDispatcher.close()
    }

    private fun createTask(priority: Priority, isExtended: Boolean, action: suspend () -> State) = when (isExtended) {
        true -> ExtendedTask(priority, this, action)
        false -> BasicTask(priority, this, action)
    }

    suspend fun enqueueBasicTask(priority: Priority, action: suspend () -> State) {
        val task = createTask(priority, false, action)
        scheduler.offer(task)
    }

    suspend fun enqueueExtendedTask(priority: Priority, action: suspend () -> State) {
        val task = createTask(priority, true, action)
        scheduler.offer(task)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}

const val DEFAULT_PROCESSOR_CORES = 1