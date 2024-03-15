interface TaskInterface {
    fun taskPreempt()
    fun taskStart()
    fun taskTerminate()
    fun taskActivate()
    fun taskWait()
    fun taskRelease()
}