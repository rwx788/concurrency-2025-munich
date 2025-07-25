@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)


    enum class Cas2Status {
        UNDECIDED, SUCCESS, FAILED
    }

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E? {
        // TODO: the cell can store a descriptor
        val value = array[index]
        return when {
            value is AtomicArrayWithCAS2<*>.CAS2Descriptor -> {
                when (value.status.get()) {
                    Cas2Status.SUCCESS -> (if (index == value.index1) value.update1 else value.update2) as E
                    Cas2Status.FAILED, Cas2Status.UNDECIDED -> (if (index == value.index1) value.expected1 else value.expected2) as E
                }
            }
            value is AtomicArrayWithCAS2<*>.DcssDescriptor -> {
                value.complete()
                return get(index)
            }
            else -> value as E
        }
    }

    fun cas2(
        index1: Int, expected1: E?, update1: E?,
        index2: Int, expected2: E?, update2: E?
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // TODO: this implementation is not linearizable,
        // TODO: Store a CAS2 descriptor in array[index1].
        val descriptor = if (index1 <= index2)
            CAS2Descriptor(
                index1 = index1, expected1 = expected1, update1 = update1,
                index2 = index2, expected2 = expected2, update2 = update2
            ) else
            CAS2Descriptor(
                index1 = index2, expected1 = expected2, update1 = update2,
                index2 = index1, expected2 = expected1, update2 = update1
            )
        descriptor.apply()
        return descriptor.status.get() === Cas2Status.SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int,
        val expected1: E?,
        val update1: E?,
        val index2: Int,
        val expected2: E?,
        val update2: E?
    ) {
        val status = AtomicReference(Cas2Status.UNDECIDED)

        fun apply() {
            val success = installDescriptor()
            applyLogically(success)
            applyPhysically()
        }

        private fun installDescriptor(index: Int, expected: E?): Boolean {
            while (true) {
                val curValue = array[index]
                when {
                    curValue == this -> return true
                    status.get() != Cas2Status.UNDECIDED -> return status.get() === Cas2Status.SUCCESS
                    curValue is AtomicArrayWithCAS2<*>.CAS2Descriptor -> {
                        curValue.apply()
                        continue
                    }
                    curValue is AtomicArrayWithCAS2<*>.DcssDescriptor -> {
                        curValue.complete()
                        continue
                    }

                    curValue != expected -> return false
                }

                if (dcss(index, expected, this, status, Cas2Status.UNDECIDED))
                    return true
            }
        }

        private fun installDescriptor(): Boolean {
            // TODO: Install this descriptor to the cells,
            // TODO: returning `true` on success, and `false`
            // TODO: if one of the cells contained an unexpected value.
            return installDescriptor(index1, expected1) && installDescriptor(index2, expected2)
        }

        private fun applyLogically(success: Boolean) {
            // TODO: Apply this CAS2 operation logically
            // TODO: by updating the descriptor status.
            if (success)
                status.compareAndSet(Cas2Status.UNDECIDED, Cas2Status.SUCCESS)
            else
                status.compareAndSet(Cas2Status.UNDECIDED, Cas2Status.FAILED)
        }

        private fun applyPhysically() {
            // TODO: Apply this operation physically
            // TODO: by updating the cells to either
            // TODO: update values (on success)
            // TODO: or back to expected values (on failure).
            val status = status.get()
            when (status) {
                Cas2Status.FAILED -> {
                    array.compareAndSet(index1, this, expected1)
                }

                Cas2Status.SUCCESS -> {
                    array.compareAndSet(index1, this, update1)
                    array.compareAndSet(index2, this, update2)
                }

                Cas2Status.UNDECIDED -> throw IllegalStateException("The status should be either SUCCESS or FAILED.")
            }
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
        index: Int,
        expectedCellState: Any?,
        updateCellState: Any?,
        statusReference: AtomicReference<*>,
        expectedCas2Status: Cas2Status
    ): Boolean {
        val descriptor = DcssDescriptor(index, expectedCellState, updateCellState,statusReference, expectedCas2Status)
        if (descriptor.install())
            descriptor.complete()

        return descriptor.status.get() == DCSSStatus.SUCCESS
    }

    private inner class DcssDescriptor(
        val index: Int,
        val expectedCellState: Any?,
        val updateCellState: Any?,
        val cas2StatusReference: AtomicReference<*>,
        val expectedStatus: Cas2Status
    ) {
        val status = AtomicReference(DCSSStatus.UNDECIDED)

        fun install(): Boolean {
            // TODO: Install descriptor to `a`
            // TODO: returning `true` on success
            // TODO: or `false` if the value is not the expected one.
            // TODO: Importantly, other threads should not help install the descriptor!
            while (true) {
                val curValue = array[index]
                when (curValue) {
                    this -> return true
                    expectedCellState -> if (array.compareAndSet(index,curValue, this)) {
                        return true
                    }

                    is AtomicArrayWithCAS2<*>.DcssDescriptor -> {
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
            val currentCas2Status = cas2StatusReference.get()

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
                array.compareAndSet(index,this, updateCellState)
            } else if (finalStatus == DCSSStatus.FAILED) {
                array.compareAndSet(index,this, expectedCellState)
            }
        }
    }

    enum class DCSSStatus {
        UNDECIDED, SUCCESS, FAILED
    }


}