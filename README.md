# Intensive Course on Concurrent Programming

## Day 0

Please clone this repo, open in IntelliJ IDEA,
and run [`test/day0/CounterTest`](test/day0/CounterTest.kt).

Please do that either directly form IntelliJ IDEA or with the following Gradle command:

* `./gradlew test --tests CounterTest` on Linux or MacOS
* `gradlew test --tests CounterTest` on Windows

<details>
<summary>Make sure the test fails with an output similar to this one (click me!)</summary>

```
AssertionFailedError: expected:<2> but was:<1>

The following interleaving leads to the error:
| ------------------------------------------------------------------------------- |
|                   Main Thread                   |   Thread 1    |   Thread 2    |
| ------------------------------------------------------------------------------- |
| thread(block = Lambda#2): Thread#1              |               |               |
| thread(block = Lambda#3): Thread#2              |               |               |
| switch (reason: waiting for Thread 1 to finish) |               |               |
|                                                 |               | run()         |
|                                                 |               |   counter ➜ 0 |
|                                                 |               |   switch      |
|                                                 | run()         |               |
|                                                 |   counter ➜ 0 |               |
|                                                 |   counter = 1 |               |
|                                                 |               |   counter = 1 |
| Thread#1.join()                                 |               |               |
| Thread#2.join()                                 |               |               |
| counter.element ➜ 1                             |               |               |
| assertEquals(2, 1): threw AssertionFailedError  |               |               |
| ------------------------------------------------------------------------------- |
```
</details>


## Day 1

### Coarse-grained bank

In [`src/day1/CoarseGrainedBank.kt`](src/day1/CoarseGrainedBank.kt),
make the sequential bank implementation thread-safe.
Please follow the *coarse-grained locking* scheme to make synchronization efficient.
For that, you need to use a single lock to protect all bank operations.

To test your solution, please run:

* `./gradlew test --tests CoarseGrainedBankTest` on Linux or MacOS
* `gradlew test --tests CoarseGrainedBankTest` on Windows

### Fine-grained bank

In [`src/day1/FineGrainedBank.kt`](src/day1/FineGrainedBank.kt),
make the sequential bank implementation thread-safe.
Please follow the *fine-grained locking* scheme to make synchronization efficient.
For that, you need to use per-account locks, thus, ensuring natural parallelism
when accessing different accounts. The `totalAmount()` function should acquire
all the locks to get a consistent snapshot, while `transfer(..)` should acquire
the corresponding account locks.

To test your solution, please run:

* `./gradlew test --tests FineGrainedBankTest` on Linux or MacOS
* `gradlew test --tests FineGrainedBankTest` on Windows

### Treiber stack

In [`src/day1/TreiberStack.kt`](src/day1/TreiberStack.kt),
implement the classic Treiber stack algorithm.

To test your solution, please run:

* `./gradlew test --tests TreiberStackTest` on Linux or MacOS
* `gradlew test --tests TreiberStackTest` on Windows

### Treiber stack with elimination

In [`src/day1/TreiberStackWithElimination.kt`](src/day1/TreiberStackWithElimination.kt),
implement the classic Treiber stack algorithm with the elimination technique.

To test your solution, please run:

* `./gradlew test --tests TreiberStackWithEliminationTest` on Linux or MacOS
* `gradlew test --tests TreiberStackWithEliminationTest` on Windows

### Michael-Scott queue

In [`src/day1/MSQueue.kt`](src/day1/MSQueue.kt),
implement the Michael-Scott queue algorithm.
You might also be interested in the [original paper](http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf).

To test your solution, please run:

* `./gradlew test --tests MSQueueTest` on Linux or MacOS
* `gradlew test --tests MSQueueTest` on Windows


### Rendezvous channel (OPTIONAL)

In [`src/day1/RendezvousChannel.kt`](src/day1/RendezvousChannel.kt), implement a rendezvous channel for Kotlin Coroutines.
Please follow the `TODO`s in the code.

* `./gradlew test --tests RendezvousChannelTest` on Linux or MacOS
* `gradlew test --tests RendezvousChannelTest` on Windows


## Day 2

### Atomic Counters Snapshot

In [`src/day2/CountersSnapshot.kt`](src/day2/CountersSnapshot.kt),
modify the `countersSnapshot()` function to make the snapshot atomic with concurrent increments.
Use the double-collect technique.

To test your solution, please run:

* `./gradlew test --tests CountersSnapshotTest` on Linux or MacOS
* `gradlew test --tests CountersSnapshotTest` on Windows


### FAA-based queue: simplified

In [`src/day2/FAABasedQueueSimplified.kt`](src/day2/FAABasedQueueSimplified.kt),
implement a concurrent queue that leverages the `Fetch-and-Add` synchronization primitive.
The high-level design of this queue bases on a conceptually infinite array for storing elements and manipulates
`enqIdx` and `deqIdx` counters, which reference the next working cells in the infinite array for `enqueue(..)`
and `dequeue()` operations.

In this task, use a big plain array as the infinite array implementation.

To test your solution, please run:

* `./gradlew test --tests FAABasedQueueSimplifiedTest` on Linux or MacOS
* `gradlew test --tests FAABasedQueueSimplifiedTest` on Windows


### FAA-based queue

In [`src/day2/FAABasedQueue.kt`](src/day2/FAABasedQueue.kt),
implement a concurrent queue that leverages the `Fetch-and-Add` synchronization primitive.
The high-level design of this queue bases on a conceptually infinite array for storing elements and manipulates
`enqIdx` and `deqIdx` counters, which reference the next working cells in the infinite array for `enqueue(..)`
and `dequeue()` operations.

The infinite array implementation should be simulated via a linked list of
fixed-size segments. The overall algorithm should be obstruction-free or lock-free.

To test your solution, please run:

* `./gradlew test --tests FAABasedQueueTest` on Linux or MacOS
* `gradlew test --tests FAABasedQueueTest` on Windows


### Logical removals in Michael-Scott queue

In [`src/day2/MSQueueWithOnlyLogicalRemove.kt`](src/day2/MSQueueWithOnlyLogicalRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should remove elements only logically, keeping the corresponding nodes
in the linked list physically, but marking them as removed.

To test your solution, please run:

* `./gradlew test --tests MSQueueWithOnlyLogicalRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithOnlyLogicalRemoveTest` on Windows


### Linear-time non-parallel removals in Michael-Scott queue

In [`src/day2/MSQueueWithLinearTimeNonParallelRemove.kt`](src/day2/MSQueueWithLinearTimeNonParallelRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should find the first node that contains the specified element
in linear time and then remove this node also in linear time.

Note that in this task `remove(..)` operations are never called in parallel, which simplifies the implementation.

To test your solution, please run:

* `./gradlew test --tests MSQueueWithLinearTimeNonParallelRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithLinearTimeNonParallelRemoveTest` on Windows


### Linear-time removals in Michael-Scott queue

In [`src/day2/MSQueueWithLinearTimeRemove.kt`](src/day2/MSQueueWithLinearTimeRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should find the first node that contains the specified element
in linear time and then remove this node also in linear time.

To test your solution, please run:

* `./gradlew test --tests MSQueueWithLinearTimeRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithLinearTimeRemoveTest` on Windows


### Constant-time removals in Michael-Scott queue

In [`src/day2/MSQueueWithConstantTimeRemove.kt`](src/day2/MSQueueWithConstantTimeRemove.kt),
implement a Michael-Scott queue with an additional `remove(element)` operation.
The implementation should find the first node that contains the specified element
in linear time, but remove this node in _constant_ time.

* `./gradlew test --tests MSQueueWithConstantTimeRemoveTest` on Linux or MacOS
* `gradlew test --tests MSQueueWithConstantTimeRemoveTest` on Windows


## Day 3

### CAS2: Reentrant Lock

In [`src/day3/AtomicArrayWithCas2OnReentrantLock.kt`](src/day3/AtomicArrayWithCas2OnReentrantLock.kt),
implement the `cas2(..)` and `get(..)` operations 
using the fine-grained locking technique to protect the array cells.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2OnReentrantLockTest` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2OnReentrantLockTest` on Windows


### CAS2: "Locked" State

In [`src/day3/AtomicArrayWithCAS2OnLockedState.kt`](src/day3/AtomicArrayWithCAS2OnLockedState.kt),
implement the `cas2(..)` and `get(..)` operations.
Use a special "Locked" value to mark cell as locked,
actively waiting for the cell state change in other operations.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2OnLockedStateTest` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2OnLockedStateTest` on Windows


### CAS2: Single-Writer

In [`src/day3/AtomicArrayWithCAS2SingleWriter.kt`](src/day3/AtomicArrayWithCAS2SingleWriter.kt),
implement the `cas2(..)` and `get(..)` operations using descriptors.
In this task, `cas2(..)` can be called only in one thread,
so concurrent `cas2(..)` invocations are forbidden.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2SingleWriterTest` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2SingleWriterTest` on Windows


### CAS2: With Implemented DCSS

In [`src/day3/AtomicArrayWithCAS2AndImplementedDCSS.kt`](src/day3/AtomicArrayWithCAS2AndImplementedDCSS.kt),
implement the `cas2(..)` operation.
Unlike in the "CAS2: Simplified" task, updates are no longer unique.
This can lead to the ABA problem. To solve it, please use
the already implemented Double-Compare-Single-Set operation when installing CAS2 descriptors.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2AndImplementedDCSSTest` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2AndImplementedDCSSTest` on Windows


### Double-Compare-Single-Set

In [`src/day3/DoubleCompareSingleSet.kt`](src/day3/DoubleCompareSingleSet.kt),
implement the `getCell()` and `dcss(..)` operations.
Use a `DcssDescriptor` to apply the `dcss(..)` operation.
Other threads should help this descriptor to finish if they observe it in `cell`.

To test your solution, please run:

* `./gradlew test --tests DoubleCompareSingleSetTest` on Linux or MacOS
* `gradlew test --tests DoubleCompareSingleSetTest` on Windows


### CAS2

In [`src/day3/AtomicArrayWithCAS2.kt`](src/day3/AtomicArrayWithCAS2.kt),
implement the `cas2(..)` operation.
Unlike in the "CAS2: Simplified" task, updates are no longer unique.
This can lead to the ABA problem. To solve it, please use
the Double-Compare-Single-Set operation when installing CAS2 descriptors.
Use your DCSS implementation from the previous assignment.

To test your solution, please run:

* `./gradlew test --tests AtomicArrayWithCAS2Test` on Linux or MacOS
* `gradlew test --tests AtomicArrayWithCAS2Test` on Windows


## Day 4

### Flat-combining queue

In [`src/day4/FlatCombiningQueue.kt`](src/day4/FlatCombiningQueue.kt), implement a concurrent queue via the
_flat-combining_ technique,
using a sequential queue under the hood. You might be interested in the corresponding
[academic paper](https://dl.acm.org/doi/pdf/10.1145/1810479.1810540)

To test your solution, please run:

* `./gradlew test --tests FlatCombiningQueueTest` on Linux or MacOS
* `gradlew test --tests FlatCombiningQueueTest` on Windows


### Single-Writer Hash Table

In [`src/day4/SingleWriterHashTable.kt`](src/day4/SingleWriterHashTable.kt), implement a concurrent hash table
that works under the "single-writer" assumption. This way, `put(..)` and `remove(..)` can be called only in one thread,
so only `get(..)`-s can be called concurrently.

To test your solution, please run:

* `./gradlew test --tests SingleWriterHashTableTest` on Linux or MacOS
* `gradlew test --tests SingleWriterHashTableTest` on Windows


### Concurrent Hash Table without Resize

In [`src/day4/ConcurrentHashTableWithoutResize.kt`](src/day4/ConcurrentHashTableWithoutResize.kt),
implement a concurrent hash table that never requires resizing.
Copy the code from [`src/day4/SingleWriterHashTable.kt`](src/day4/SingleWriterHashTable.kt)
and use `CAS`-s to perform all updates.

To test your solution, please run:

* `./gradlew test --tests ConcurrentHashTableWithoutResizeTest` on Linux or MacOS
* `gradlew test --tests ConcurrentHashTableWithoutResizeTest` on Windows


### Concurrent Hash Table

In [`src/day4/ConcurrentHashTable.kt`](src/day4/ConcurrentHashTable.kt),
implement a concurrent hash table without restrictions. Enjoy the final task!

To test your solution, please run:

* `./gradlew test --tests ConcurrentHashTableTest` on Linux or MacOS
* `gradlew test --tests ConcurrentHashTableTest` on Windows

### Lincheck Tests

Please complete the Lincheck tests in [`test/day4/LincheckTests.kt`](test/day4/LincheckTests.kt)
following the `TODO`s. Make sure these tests reveal the described bugs.  
