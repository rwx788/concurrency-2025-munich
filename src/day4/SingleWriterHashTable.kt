@file:Suppress("UNCHECKED_CAST")

package day4

import java.util.concurrent.atomic.*
import kotlin.math.*

class SingleWriterHashTable<K : Any, V : Any>(initialCapacity: Int) : HashTable<K, V> {
    private val table = AtomicReference(Table<K, V>(initialCapacity))

    override fun put(key: K, value: V): V? {
        while (true) {
            // Try to insert the key/value pair.
            table.get().put(key, value).let {
                if (it === NEEDS_RESIZE) {
                    // The current table is too small to insert a new key.
                    // Create a new table of x2 capacity,
                    // copy all elements to it,
                    // and restart the current operation.
                    resize()
                } else {
                    // The operation has been successfully performed,
                    // return the previous value associated with the key.
                    return it as V?
                }
            }
        }
    }

    override fun get(key: K): V? = table.get().get(key)

    override fun remove(key: K): V? = table.get().remove(key)

    private fun resize() {
        // Get a reference to the current table.
        val curCore = table.get()
        // Create a new table of x2 capacity.
        val newTable = Table<K, V>(curCore.capacity * 2)
        // Copy all elements from the current table to the new one.
        repeat(curCore.capacity) { index ->
            val key = curCore.keys[index]
            val value = curCore.values[index]
            // Is the cell non-empty and does a value present?
            if (key != null && key != REMOVED_KEY && value != null) {
                newTable.put(key as K, value)
            }
        }
        // Replace the current table with the new one.
        table.set(newTable)
    }

    class Table<K : Any, V : Any>(val capacity: Int) {
        val keys = AtomicReferenceArray<Any?>(capacity)
        val values = AtomicReferenceArray<V?>(capacity)

        fun put(key: K, value: V): Any? {
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
                        val oldValue = values[index]
                        values[index] = value
                        return oldValue
                    }
                    // The cell does not store a key.
                    null, REMOVED_KEY -> {
                        // Insert the key/value pair into this cell.
                        keys[index] = key
                        values[index] = value
                        // No value was associated with the key.
                        return null
                    }
                }
                // Process the next cell, use linear probing.
                index = (index + 1) % capacity
            }
            // Inform the caller that the table should be resized.
            return NEEDS_RESIZE
        }

        fun get(key: K): V? {
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

        fun remove(key: K): V? {
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
                        // keys[index] = REMOVED_KEY
                        // Read the value associated with the key and replace it with `null`.
                        val oldValue = values[index]
                        values[index] = null
                        return oldValue
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

        private fun index(key: Any) = ((key.hashCode() * MAGIC) % capacity).absoluteValue
    }
}

private const val MAGIC = -0x61c88647 // golden ratio
private const val MAX_PROBES = 2 // DO NOT CHANGE THIS CONSTANT
val NEEDS_RESIZE = Any()

// TODO: Once a table cell is associated with a key,
// TODO: it should be associated with it forever.
// TODO: This way, `remove()` should only set `null` to the value slot,
// TODO: without replacing the key slot with `REMOVED_KEY`.
val REMOVED_KEY = Any()