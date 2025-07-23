package day2

import java.util.concurrent.atomic.*

class CountersSnapshot {
    val counter1 = AtomicLong(0)
    val counter2 = AtomicLong(0)
    val counter3 = AtomicLong(0)

    fun incrementCounter1() = counter1.getAndIncrement()
    fun incrementCounter2() = counter2.getAndIncrement()
    fun incrementCounter3() = counter3.getAndIncrement()

    fun countersSnapshot(): Triple<Long, Long, Long> {

        while (true) {
            val first = counter1.get()
            val second = counter2.get()
            val third = counter3.get()

            if (first != counter1.get()  ||
                second != counter2.get()
            ) continue

            return Triple(first, second, third)
        }

    }
}