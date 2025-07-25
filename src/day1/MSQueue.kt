package day1

import java.util.concurrent.atomic.AtomicReference

class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element)
        while (true) {
            val curTail = tail.get()
            val curTailNext = curTail.next
            if (curTailNext.compareAndSet(null, newNode)) {
                tail.compareAndSet(curTail, newNode)
                return
            }
            // Move tail to the actual state of the list
            tail.compareAndSet(curTail, curTail.next.get())
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val curTail = tail.get()
            val newHead = curHead.next.get()
            if (curHead == curTail) {
                if (newHead == null) {
                    return null
                } else {
                    tail.compareAndSet(curTail, curTail.next.get())
                }
            }
            if (head.compareAndSet(curHead, newHead)) {
                val rez = newHead?.element
                newHead?.element = null
                return rez
            }
        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "`tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "`head.element` must be `null`"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
