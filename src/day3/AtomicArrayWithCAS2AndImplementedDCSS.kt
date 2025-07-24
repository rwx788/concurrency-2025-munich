@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2AndImplementedDCSS.Status.*
import day3.AtomicArrayWithCAS2SingleWriter.UpdateTuple
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2AndImplementedDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        // TODO: Copy the implementation from `AtomicArrayWithCAS2Simplified`
        val value = array[index]
        return when {
            value is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor -> {
                when (value.status.get()) {
                    SUCCESS -> (if (index == value.index1) value.update1 else value.update2) as E
                    FAILED, UNDECIDED -> (if (index == value.index1) value.expected1 else value.expected2) as E
                }
            }

            else -> value as E
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }

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
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int,
        val expected1: E,
        val update1: E,
        val index2: Int,
        val expected2: E,
        val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            val success = installDescriptor()
            applyLogically(success)
            applyPhysically()
        }

        private fun helpDescriptor(descr: Any?): Boolean {
            if (descr is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor && descr != this) {
                if (descr.status.get() == UNDECIDED)
                    descr.apply()
                else
                    descr.applyPhysically()

                return true
            }
            return false
        }

        private fun installDescriptor(index: Int, expected: E): Boolean {
            while (true) {
                val curValue = array[index]
                when {
                    curValue == this -> return true
                    status.get() != UNDECIDED -> return status.get() === SUCCESS
                    curValue is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor -> {
                        curValue.apply()
                        continue
                    }
                    curValue != expected -> return false
                }

                if (dcss(index, expected, this, status, UNDECIDED))
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
                status.compareAndSet(UNDECIDED, SUCCESS)
            else
                status.compareAndSet(UNDECIDED, FAILED)
        }

        private fun applyPhysically() {
            // TODO: Apply this operation physically
            // TODO: by updating the cells to either
            // TODO: update values (on success)
            // TODO: or back to expected values (on failure).
            val status = status.get()
            when (status) {
                FAILED -> {
                    array.compareAndSet(index1, this, expected1)
                }

                SUCCESS -> {
                    array.compareAndSet(index1, this, update1)
                    array.compareAndSet(index2, this, update2)
                }

                UNDECIDED -> throw IllegalStateException("The status should be either SUCCESS or FAILED.")
            }
        }
    }
    private fun isDescriptor(descr: Any?): Boolean {
        return descr is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }

    // TODO: Please use this DCSS implementation to ensure that
    // TODO: the status is `UNDECIDED` when installing the descriptor.
    // TODO: DO NOT RENAME THIS METHOD
    fun dcss(
        index: Int,
        expectedCellState: Any?,
        updateCellState: Any?,
        statusReference: AtomicReference<*>,
        expectedStatus: Any?
    ): Boolean =
        if (array[index] == expectedCellState && statusReference.get() == expectedStatus) {
            array[index] = updateCellState
            true
        } else {
            false
        }
}