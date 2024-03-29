package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.atomic.AtomicInteger

abstract class OperatingSystem(private val processorCores: Int = DEFAULT_PROCESSOR_CORES) : AutoCloseable {
    protected abstract val scheduler: Scheduler
    private var isActive = true
    private val id = OperatingSystem.id.getAndIncrement()

    private val processorDispatcher = newFixedThreadPoolContext(processorCores, "processor")
    private val osDispatcher = newSingleThreadContext("OS")

    suspend fun start() = coroutineScope {
        logger.debug { "OS is started | Waiting for a task to execute" }
        launch(osDispatcher) {
            var task = scheduler.pop(this@OperatingSystem).await()
            logger.debug { "Processor received task to execute $task" }

            while (isActive && processorDispatcher.isActive) {

                val job = async(processorDispatcher) { task.onEvent(Event.Start) }
                task.preemptIfCaseOfHigherPriorityTask(job)

                val state = try {
                    job.await()
                } catch (e: CancellationException) {
                    require(task.state is State.Ready) // after preempt
                    val higherPriorityTask = scheduler.pop(this@OperatingSystem).await()
                    scheduler.offer(task, this@OperatingSystem)
                    logger.debug { "Swap running task $task with a higher priority one: $higherPriorityTask" }
                    task = higherPriorityTask
                    scheduler.unlock(this@OperatingSystem)
                    continue
                }

                when (state) {
                    is State.Suspended -> {
                        logger.debug { "$task finished execution" }
                        task = scheduler.pop(this@OperatingSystem).await()
                    }

                    is ExtendedState.Waiting -> {
                        logger.debug { "$task is waiting for an event" }
                        launch(Dispatchers.IO) {
                            require(task.onEvent(Event.Release) is State.Ready)
                            scheduler.offer(task, this@OperatingSystem)
                        }
                        task = scheduler.pop(this@OperatingSystem).await()
                    }

                    else -> throw IllegalStateException("$task")
                }
            }
        }
    }

    private suspend fun Task.preemptIfCaseOfHigherPriorityTask(job: Job) {
        while (job.isActive) { // higher priority task check
            if (scheduler.hasHigherPriorityTask(priority)) {
                scheduler.lock(this@OperatingSystem)
                require(onEvent(Event.Preempt) is State.Ready)
                break
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
        scheduler.offer(task, this)
    }

    suspend fun enqueueExtendedTask(priority: Priority, action: suspend () -> State) {
        val task = createTask(priority, true, action)
        scheduler.offer(task, this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val id = AtomicInteger(0)
        const val DEFAULT_PROCESSOR_CORES = 1
    }

    override fun equals(other: Any?): Boolean {
        if (other is OperatingSystem)
            return this.id == other.id
        return false
    }

    override fun toString(): String = "Operating System"
}
