package edu.shamalov.os

import edu.shamalov.os.schedule.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.*
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
        logger.debug { "OS is started | Waiting for a task to execute" }
        job = launch(osDispatcher) {
            var task = scheduler.pop(this@OperatingSystem).await()

            while (this.isActive) {
                val job = with(processor) { execute(task) }
                launch(Dispatchers.IO) { task.preemptIfCaseOfHigherPriorityTask(job) }

                val state = try {
                    job.await()
                } catch (e: CancellationException) {
                    if (e.message?.contains("Preempt") != true)
                        continue

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
        scheduler.close()
        processor.close()
        job.cancel()
        osDispatcher.close()
    }

    fun stop() = close()

    suspend fun enqueueTask(task: Task) {
        scheduler.offer(task, this)
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
