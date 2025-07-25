package day4

import day1.Queue
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReferenceArray

open class FlatCombiningQueueWithTrickyBug<E : Any> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E): Unit = enqueueOrDequeue(task = element) {
        nonConcurrentEnqueue(element)
    }

    override fun dequeue(): E? = enqueueOrDequeue(task = Dequeue) {
        nonConcurrentDequeue()
    }

    private fun <R> enqueueOrDequeue(task: Any, operation: () -> R): R {
        var operationIndex: Int = -1
        while (true) {
            // Make this code thread-safe using the flat-combining technique.
            // 1.  Try to become a combiner by changing `combinerLock` from `false` (unlocked) to `true` (locked).
            if (tryAcquireLock()) {
                try {
                    // 2a. On success, apply this operation
                    val operationResult = applyOperation(operationIndex, operation)

                    // and help others by traversing `tasksForCombiner`, performing the announced operations, and updating the corresponding cells to `Result`.
                    // Put the corresponding helping object into `helpOthers()`.
                    helpOthers()

                    return operationResult
                } finally {
                    releaseLock()
                }
            } else {
                // 2b. If the lock is already acquired, announce this operation in `tasksForCombiner`
                //     by replacing a random cell state from `null` with task.
                if (operationIndex < 0) {
                    operationIndex = storeOperation(task)
                } else {
                    // Operation already set maybe it was updated to `Result`
                    val operationState = tasksForCombiner.get(operationIndex)
                    if (operationState is Result<*>) {
                        // (do not forget to clean it in this case), or `combinerLock` becomes available to acquire.
                        tasksForCombiner.set(operationIndex, null)
                        return operationState.value as R
                    }
                }
            }
        }
    }

    private fun helpOthers() {
        // Traverse `tasksForCombiner` and perform the announced operations,
        for (i in (0 until TASKS_FOR_COMBINER_SIZE)) {
            val taskValue = tasksForCombiner.get(i)
            val operationResult: Any? = when (taskValue) {
                null -> continue
                is Result<*> -> continue
                is Dequeue -> Result(nonConcurrentDequeue())
                else -> Result(nonConcurrentEnqueue(taskValue as E))
            }

            // updating the corresponding cells to `Result`.
            tasksForCombiner.set(i, operationResult)
        }
    }

    private fun storeOperation(operation: Any?): Int = randomCellIndex().let { operationIndex ->
        if (tasksForCombiner.compareAndSet(operationIndex, null, operation)) {
            operationIndex
        } else {
            -1
        }
    }

    private fun nonConcurrentDequeue() = queue.removeFirstOrNull()

    private fun nonConcurrentEnqueue(element: E) = queue.add(element)

    private fun tryAcquireLock(): Boolean =
        // Try to acquire combinerLock by changing `combinerLock` from `false` (unlocked) to `true` (locked).
        combinerLock.compareAndSet(false, true)

    open fun releaseLock() =
        // Release combinerLock by changing `combinerLock` to `false` (unlocked).
        combinerLock.set(false)

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())

    private fun <R> applyOperation(operationIndex: Int, operation: () -> R): R =
        if (operationIndex < 0) {
            operation()
        } else {
            val operationState = tasksForCombiner.get(operationIndex)
            if (operationState is Result<*>) {
                tasksForCombiner.set(operationIndex, null)
                operationState.value as R
            } else {
                tasksForCombiner.set(operationIndex, null)
                operation()
            }
        }

    // Put this token in `tasksForCombiner` for dequeue().
    // enqueue()-s should put the inserting element.
    private object Dequeue

    // Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
    private class Result<V>(
        val value: V
    )
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!