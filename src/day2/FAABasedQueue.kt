package day2

import day1.*
import java.util.concurrent.atomic.*

class InfiniteArray {
    private val head = Segment(0)

    private fun getSegmentByIndex(i: Int): Int {
        return i / SEGMENT_SIZE
    }

    private fun getIndexInSegment(i: Int): Int {
        return i % SEGMENT_SIZE
    }

    private fun findSegmentWithId(id: Int): Segment {
        var currentSegment: Segment = head
        while (true) {
            if(currentSegment.id == id.toLong())
                return currentSegment
            val nextSegment = currentSegment.next.get()
            // There is a next segment, keep looking
            if (nextSegment != null) {
                currentSegment = nextSegment
                continue
            }
            // No segment found so create a new ones
            else
            {
                for(i in currentSegment.id.toInt() + 1 .. id) {
                    currentSegment.next.compareAndSet(null, Segment(i.toLong()))
                }
                return currentSegment.next.get()!!
            }
        }
    }

    fun compareAndSet(i: Int, expectedValue: Any?, newValue: Any?): Boolean {
        val segment = findSegmentWithId(getSegmentByIndex(i))
        return segment.cells.compareAndSet(getIndexInSegment(i), expectedValue, newValue)
    }

    fun get(i: Int): Any? {
        val segment = findSegmentWithId(getSegmentByIndex(i))
        return segment.cells.get(getIndexInSegment(i))
    }
}

// TODO: Copy the code from `FAABasedQueueSimplified`
// TODO: and implement the infinite array on a linked list
// TODO: of fixed-size `Segment`s.
class FAABasedQueue<E> : Queue<E> {
    private val infiniteArray = InfiniteArray() // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            // TODO: Increment the counter atomically via Fetch-and-Add.
            // TODO: Use `getAndIncrement()` function for that.
            val i = enqIdx.getAndIncrement()
            // TODO: Atomically install the element into the cell
            // TODO: if the cell is not poisoned.
            if (infiniteArray.compareAndSet(i.toInt(), null, element))
                return
        }
    }

    fun isQueueEmpty(): Boolean {
        while (true) {
            val enq = enqIdx.get()
            val deq = deqIdx.get()
            if (enq != enqIdx.get()) continue
            return deq >= enq
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            // Is this queue empty?
            if (isQueueEmpty()) return null
            // TODO: Increment the counter atomically via Fetch-and-Add.
            // TODO: Use `getAndIncrement()` function for that.
            val i = deqIdx.getAndIncrement().toInt()

            // TODO: Try to retrieve an element if the cell contains an
            // TODO: element, poisoning the cell if it is empty.
            if (infiniteArray.compareAndSet(i, null, POISONED)) {
                continue
            }
            val value = infiniteArray.get(i) as E
            if (infiniteArray.compareAndSet(i, value, null))
                return value
        }
    }
}

// TODO: Use me to construct a linked list of segments.
private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2

// TODO: Use me to mark a cell poisoned.
private val POISONED = Any()
