package day3

import java.util.concurrent.atomic.*

/**
 *  Provides atomic operations for managing Cell and CAS2 Status fields with
 * a Double Compare Single Set (DCSS) operation to change the Cell state.
 *
 * @param E The type of Cell values.
 */
class DoubleCompareSingleSet<E>(initialCellValue: E) {
    private val cell = AtomicReference<Any>(initialCellValue)
    private val cas2status = AtomicReference(Cas2Status.UNDECIDED)

    /**
     * Reads the Cell value.
     */
    fun getCell(): E {
        // 'cell' can store DCSSDescriptor
        while (true) {
            val curValue = cell.get()
            if (curValue is DoubleCompareSingleSet<*>.DcssDescriptor) {
                curValue.complete()
                continue
            }
            return curValue as E
        }
    }

    /**
     * Performs a Double Compare Single Set operation.
     * Atomically updates the Cell value to [updateCellState]
     * if it equals [expectedCellState] and `cas2status` equals [expectedStatus].
     *
     * @return true if the operation was successful, false otherwise
     */
    fun dcss(
        expectedCellState: E, updateCellState: E,
        expectedCas2Status: Cas2Status
    ): Boolean {
        val descriptor = DcssDescriptor(expectedCellState, updateCellState, expectedCas2Status)
        if (descriptor.install())
            descriptor.complete()

        return descriptor.status.get() == DCSSStatus.SUCCESS
    }

    private inner class DcssDescriptor(
        val expectedCellState: E,
        val updateCellState: E,
        val expectedStatus: Cas2Status
    ) {
        val status = AtomicReference(DCSSStatus.UNDECIDED)

        fun install(): Boolean {
            // TODO: Install descriptor to `a`
            // TODO: returning `true` on success
            // TODO: or `false` if the value is not the expected one.
            // TODO: Importantly, other threads should not help install the descriptor!
            while (true) {
                val curValue = cell.get()
                when (curValue) {
                    this -> return true
                    expectedCellState -> if (cell.compareAndSet(curValue, this)) {
                        return true
                    }

                    is DoubleCompareSingleSet<*>.DcssDescriptor -> {
                        curValue.complete()
                        continue
                    }

                    else -> return false
                }
            }
        }

        // Other operations can call this function for helping.
        fun complete() {
            // (1) Apply logically: check whether cas2status == expectedStatus and update the descriptor status
            val currentCas2Status = getStatus()

            // Only try to update status if it's still UNDECIDED
            if (status.get() == DCSSStatus.UNDECIDED) {
                if (expectedStatus == currentCas2Status) {
                    status.compareAndSet(DCSSStatus.UNDECIDED, DCSSStatus.SUCCESS)
                } else {
                    status.compareAndSet(DCSSStatus.UNDECIDED, DCSSStatus.FAILED)
                }
            }

            // (2) Apply physically: update the cell based on the final status
            val finalStatus = status.get()
            if (finalStatus == DCSSStatus.SUCCESS) {
                cell.compareAndSet(this, updateCellState)
            } else if (finalStatus == DCSSStatus.FAILED) {
                cell.compareAndSet(this, expectedCellState)
            }
        }
    }

    enum class DCSSStatus {
        UNDECIDED, SUCCESS, FAILED
    }

    /**
     * Tries to update the Statue value from UNDECIDED to [status].
     * Returns `true` on success and `false` otherwise.
     */
    fun updateStatus(status: Cas2Status): Boolean {
        return cas2status.compareAndSet(Cas2Status.UNDECIDED, status)
    }

    /**
     * Gets the current value of B.
     */
    fun getStatus(): Cas2Status {
        return cas2status.get()
    }

    enum class Cas2Status {
        UNDECIDED, SUCCESS, FAILURE
    }
}
