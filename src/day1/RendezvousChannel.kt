package day1

import java.util.concurrent.atomic.*
import kotlin.coroutines.*

/**
 * Rendezvous channel is the key synchronization primitive behind coroutines,
 * it is also known as a synchronous queue.
 *
 * At its core, a rendezvous channel is a blocking bounded queue of zero capacity.
 * It supports `send(e)` and `receive()` requests:
 * send(e) checks whether the queue contains any receivers and either removes the first one
 * (i.e. performs a “rendezvous” with it) or adds itself to the queue as a waiting sender and suspends.
 * The `receive()` operation works symmetrically.
 *
 * This implementation never stores `null`-s for simplicity.
 *
 * (You may also follow `SequentialChannelInt` in tests for the sequential semantics).
 */
class RendezvousChannel<E : Any> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null, null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    suspend fun send(element: E) {
        while (true) {
            // TODO: feel free to change this code if needed.
            val currentHead = head.get()
            val currentTail = tail.get()
            val nextHead = currentHead.next.get()
            // Is this queue empty or contain other senders?
            if (isEmptyOrContainsSenders()) {

                val success = suspendCoroutine<Boolean> { continuation ->
                    val node = Node(element, continuation as Continuation<Any?>)
                    if (!tryAddNode(currentTail, node)) {
                        // Fail and retry.
                        continuation.resume(false)
                    }
                }
                // Finish on success and retry on failure.
                if (success) return
            } else {
                // The queue contains receivers, try to extract the first one.
                val firstReceiver = tryExtractNode(currentHead) ?: continue
                firstReceiver.continuation!!.resume(element)
                return
            }
        }
    }

    suspend fun receive(): E {
        while (true) {
            // TODO: feel free to change this code if needed.
            val currentHead = head.get()
            val currentTail = tail.get()
            val nextHead = currentHead.next.get()
            // Is this queue empty or contain other receivers?
            if (isEmptyOrContainsReceivers()) {
                val element = suspendCoroutine<E?> { continuation ->
                    val node = Node(RECEIVER, continuation as Continuation<Any?>)
                    if (!tryAddNode(currentTail, node)) {
                        // Fail and retry.
                        continuation.resume(null)
                    }
                }
                // Should we retry?
                if (element == null) continue
                // Return the element
                return element
            } else {
                // The queue contains senders, try to extract the first one.
                val firstSender = tryExtractNode(currentHead) ?: continue
                firstSender.continuation!!.resume(true)
                return firstSender.element as E
            }
        }
    }

    private fun isEmptyOrContainsReceivers(): Boolean {
        // For receivers, Node.element === RECEIVER
        val nextHead = head.get().next.get()
        return (nextHead == null || nextHead.element === RECEIVER)
    }

    private fun isEmptyOrContainsSenders(): Boolean {
        // For senders, Node.element !== RECEIVER
        return (head.get().next.get()?.element !== RECEIVER)
    }

    private fun tryAddNode(curTail: Node, newNode: Node): Boolean {
        // TODO: Return `false` if the attempt has failed.
        if (curTail.next.compareAndSet(null, newNode)) {
            tail.set(newNode)
            return true
        }

        val nextNode = curTail.next.get()
        if (nextNode != null) {
            tail.compareAndSet(curTail, nextNode)
        }
        return false
    }

    private fun tryExtractNode(curHead: Node): Node? {
        // TODO: Return a node with the extracted continuation & element.
        val newHead = curHead.next.get()

        if (head.compareAndSet(curHead, newHead)) {

            return newHead
        }

        return null
    }

    class Node(
        // Sending element in case of suspended `send()` or
        // RECEIVER in case of suspended `receive()`.
        val element: Any?,
        // Suspended `send` of `receive` request.
        val continuation: Continuation<Any?>?
    ) {
        val next = AtomicReference<Node?>(null)
    }
}

private val RECEIVER = "Receiver"