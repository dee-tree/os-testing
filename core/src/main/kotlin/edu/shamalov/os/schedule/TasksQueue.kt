package edu.shamalov.os.schedule

import edu.shamalov.os.Priority
import edu.shamalov.os.Task
import edu.shamalov.os.PRIORITY_RANGE
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger

class TasksQueue internal constructor(tasks: Map<Priority, LinkedList<Task>>) {

    private val tasks: MutableMap<Priority, LinkedList<Task>> = tasks.toMutableMap()

    constructor() : this(hashMapOf())

    init {
        synchronized(this) {
            for (priority in PRIORITY_RANGE) {
                this.tasks.getOrPut(Priority(priority)) { LinkedList() }
            }
        }
    }

    private val _size = AtomicInteger(0)

    val size: Int
        get() = _size.get()

    private val hotTasks: LinkedList<Task>
        get() = tasks.filter { (_, anyTasks) -> anyTasks.isNotEmpty() }.maxBy { (priority, _) -> priority }.value

    val maxPriority: Priority
        get() = hotTasks.first.priority

    fun isEmpty(): Boolean {
        return _size.get() == 0
    }

    /**
     * Get the first task in the queue by priority, remove it from the queue, and return it
     */
    fun pop(): Task {
        return synchronized(this) {
            if (hotTasks.isNotEmpty()) _size.getAndDecrement()
            hotTasks.pop()
        }
    }

    /**
     * Add a task to the queue
     */
    fun offer(task: Task) {
        synchronized(this) { tasks[task.priority]!!.offer(task) }
        _size.getAndIncrement()
    }
}
