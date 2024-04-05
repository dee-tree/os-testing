package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

class OperatingSystem(
    private val processor: Processor = Processor(),
    private val scheduler: Scheduler = Scheduler()
) : AutoCloseable {
    private var isActive = true
    private val id = OperatingSystem.id.getAndIncrement()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val osDispatcher = newSingleThreadContext("OS")

    private lateinit var job: Job
    fun CoroutineScope.start() {
        logger.info { "Started | Waiting for a task to execute" }
        job = launch(osDispatcher) {
            var task = scheduler.pop()

            while (this.isActive) {
                val job = coroutineScope {
                        val runningJob = with(processor) { execute(task) }

                        val preemptSearchJob = launch(Dispatchers.IO) {
                            while (runningJob.isActive && this.isActive && this@OperatingSystem.isActive) { // higher priority task check
                                if (task.state is State.Running && scheduler.hasHigherPriorityTask(task.priority)) {
                                    task.onEvent(Event.Preempt)
                                    break
                                }
                            }
                    }
                    runningJob
                }

                val state = try {
                    job.await()
                } catch (e: CancellationException) {
                    when {
                        e.message?.contains(Event.Wait.toString()) == true -> {
                            logger.debug { "$task is waiting for an event" }
                            val waitingTask = task
                            launch(Dispatchers.IO) {
                                waitingTask.onEvent(Event.Wait)
                                waitingTask.onEvent(Event.Release)
                                scheduler.offer(waitingTask)
                            }
                            task = scheduler.pop()
                            continue
                        }
                        e.message?.contains("Preempt") == true -> {
//                            require(task.state is State.Ready) // after preempt

                            scheduler.withLock {
                                val higherPriorityTask = scheduler.pop()
                                scheduler.offer(task)
                                logger.info { "Swap running task $task with a higher priority one: $higherPriorityTask" }
                                task = higherPriorityTask
                            }

                            continue
                        }
                        else -> continue
                    }
                }

                check(state is State.Suspended)
                logger.debug { "$task finished execution" }
                task = runCatching { scheduler.pop() }.getOrElse { close(); return@launch }

            }
        }
    }

    override fun close() {
        if (!isActive) return
        isActive = false
        processor.close()
        if (this::job.isInitialized) job.cancel()
        osDispatcher.close()
        scheduler.close()
    }

    fun stop() = close()

    suspend fun enqueueTask(task: Task) {
        runCatching { scheduler.offer(task) }.getOrElse { close() }
    }

    companion object {
        private val logger = KotlinLogging.logger("OS")
        private val id = AtomicInteger(0)
    }

    override fun equals(other: Any?): Boolean {
        if (other is OperatingSystem)
            return this.id == other.id
        return false
    }

    override fun hashCode(): Int = Objects.hash(id)

    override fun toString(): String = "Operating System #$id"
}
