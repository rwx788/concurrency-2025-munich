package day4

import java.util.concurrent.atomic.*
import kotlin.math.absoluteValue

class ConcurrentHashTableWithoutResize<K : Any, V : Any>(initialCapacity: Int) : HashTable<K, V> {
    private val table = AtomicReference(Table<K, V>(initialCapacity))

    override fun put(key: K, value: V): V? {
        while (true) {
            // Try to insert the key/value pair.
            val putResult = table.get().put(key, value)
            if (putResult === NEEDS_REHASH) {
                // The current table is too small to insert a new key.
                // Create a new table of x2 capacity,
                // copy all elements to it,
                // and restart the current operation.
                resize()
            } else {
                // The operation has been successfully performed,
                // return the previous value associated with the key.
                return putResult as V?
            }
        }
    }

    override fun get(key: K): V? {
        return table.get().get(key)
    }

    override fun remove(key: K): V? {
        return table.get().remove(key)
    }

    private fun resize() {
        error("Should not be called in this task")
    }

    class Table<K : Any, V : Any>(val capacity: Int) {
        val keys = AtomicReferenceArray<Any?>(capacity)
        val values = AtomicReferenceArray<V?>(capacity)

        fun put(key: K, value: V): Any? {
            // TODO: Copy your implementation from `SingleWriterHashTable`
            // TODO: and replace all writes to update key/value with CAS-s.
            var index = index(key)
            // Search for a specified key probing `MAX_PROBES` cells.
            // If neither the key nor an empty cell is found,
            // inform the caller that the table should be resized.
            repeat(MAX_PROBES) {
                // Read the key.
                val curKey = keys[index]
                when (curKey) {
                    // The cell contains the specified key.
                    key -> {
                        // Update the value and return the previous one.
                        return setValueAtomically(index, value)
                    }
                    // The cell does not store a key.
                    null -> {
                        // Insert the key/value pair into this cell.
                        if (keys.compareAndSet(index, null, key)) {
                            return setValueAtomically(index, value)
                        } else {
                            if (keys[index] == key) {
                                return setValueAtomically(index, value)
                            }
                        }
                    }
                    // The cell does not store a key.
                    REMOVED_KEY -> {
                        // We do not use this key yet
                    }
                }
                // Process the next cell, use linear probing.
                index = (index + 1) % capacity
            }
            // Inform the caller that the table should be resized.
            return NEEDS_RESIZE
        }

        fun setValueAtomically(index: Int, value: V?): V? {
            while (true) {
                val oldValue = values.get(index)
                if (values.compareAndSet(index, oldValue, value)) {
                    // No value was associated with the key.
                    return oldValue
                }
            }
        }

        fun get(key: K): V? {
            // TODO: Copy your implementation from `SingleWriterHashTable`.
            var index = index(key)
            // Search for a specified key probing `MAX_PROBES` cells.
            // If neither the key is not found after that,
            // the table does not contain it.
            repeat(MAX_PROBES) {
                // Read the key.
                val curKey = keys[index]
                when (curKey) {
                    // The cell contains the required key.
                    key -> {
                        // Read the value associated with the key.
                        return values[index]
                    }
                    // Empty cell.
                    null -> {
                        // The key has not been found.
                        return null
                    }
                }
                // Process the next cell, use linear probing.
                index = (index + 1) % capacity
            }
            // The key has not been found.
            return null
        }

        fun index(key: Any) = ((key.hashCode() * MAGIC) % capacity).absoluteValue

        fun remove(key: K): V? {
            // TODO: Copy your implementation from `SingleWriterHashTable`
            // TODO: and replace the write to update the value with CAS.
            var index = index(key)
            // Search for a specified key probing `MAX_PROBES` cells.
            // If neither the key is not found after that,
            // the table does not contain it.
            repeat(MAX_PROBES) {
                // Read the key.
                val curKey = keys[index]
                when (curKey) {
                    // The cell contains the required key.
                    key -> {
                        // TODO: Once a table cell is associated with a key,
                        // TODO: it should be associated with it forever.
                        // TODO: This way, `remove()` should only set `null` to the value slot,
                        // TODO: without replacing the key slot with `REMOVED_KEY`.

                        // Mark the slot available for `put(..)`,
                        // but do not stop on this cell when searching for a key.
                        // For that, replace the key with `REMOVED_KEY`.
                        // Read the value associated with the key and replace it with `null`.
                        return setValueAtomically(index, null)
                    }
                    // Empty cell.
                    null -> {
                        // The key has not been found.
                        return null
                    }
                }
                // Process the next cell, use linear probing.
                index = (index + 1) % capacity
            }
            // The key has not been found.
            return null
        }
    }
}

private const val MAGIC = -0x61c88647 // golden ratio
private const val MAX_PROBES = 2
private val NEEDS_REHASH = Any()