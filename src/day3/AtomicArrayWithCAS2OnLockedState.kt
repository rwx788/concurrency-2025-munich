package day3

import java.util.concurrent.atomic.*
import java.util.concurrent.locks.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2OnLockedState<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        // TODO: Cover the case when the cell state is LOCKED.
        while (true) {
            val value = array[index]
            if (value === LOCKED)
                continue
            return value as E
        }
    }

    data class UpdateTuple<T>(
        val expected: T,
        val update: T
    )

    fun lockCell(index: Int, expected: E) : Boolean {
        while (true) {
            val value = array[index]
            when {
                value === LOCKED -> continue
                value === expected -> if (array.compareAndSet(index, expected, LOCKED))
                    return true
                else -> return false
            }
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // TODO: Make me thread-safe by "locking" the cells
        // TODO: via atomically changing their states to LOCKED.
        val updates = sortedMapOf(
            index1 to UpdateTuple(expected1, update1),
            index2 to UpdateTuple(expected2, update2)
        ).toList()

        if(!lockCell(updates[0].first, updates[0].second.expected)) {
            return false
        }
        if(!lockCell(updates[1].first, updates[1].second.expected)) {
            array.set(updates[0].first, updates[0].second.expected)
            return false
        }
        array.set(updates[0].first, updates[0].second.update)
        array.set(updates[1].first, updates[1].second.update)
        return true
    }
}

// TODO: Store me in `a` to indicate that the reference is "locked".
// TODO: Other operations should wait in an active loop until the
// TODO: value changes.
private val LOCKED = "Locked"