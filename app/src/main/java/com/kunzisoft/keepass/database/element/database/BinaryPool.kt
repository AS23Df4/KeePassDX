/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.database

import java.io.File
import java.io.IOException

abstract class BinaryPool<T> {

    protected val pool = LinkedHashMap<T, BinaryFile>()

    private var binaryFileIncrement = 0L // Unique file id (don't use current time because CPU too fast)

    /**
     * To get a binary by the pool key (ref attribute in entry)
     */
    operator fun get(key: T): BinaryFile? {
        return pool[key]
    }

    /**
     * Create and return a new binary file not yet linked to a binary
     */
    fun put(cacheDirectory: File,
            key: T? = null,
            compression: Boolean = false,
            protection: Boolean = false): KeyBinary<T> {
        val fileInCache = File(cacheDirectory, binaryFileIncrement.toString())
        binaryFileIncrement++
        val newBinaryFile = BinaryFile(fileInCache, compression, protection)
        val newKey = put(key, newBinaryFile)
        return KeyBinary(newBinaryFile, newKey)
    }

    /**
     * To linked a binary with a pool key, if the pool key doesn't exists, create an unused one
     */
    fun put(key: T?, value: BinaryFile): T {
        if (key == null)
            return put(value)
        else
            pool[key] = value
        return key
    }

    /**
     * To put a [binaryFile] in the pool,
     * if already exists, replace the current one,
     * else add it with a new key
     */
    fun put(binaryFile: BinaryFile): T {
        var key: T? = findKey(binaryFile)
        if (key == null) {
            key = findUnusedKey()
        }
        pool[key!!] = binaryFile
        return key
    }

    /**
     * Remove a binary from the pool, the file is not deleted
     */
    @Throws(IOException::class)
    fun remove(binaryFile: BinaryFile) {
        findKey(binaryFile)?.let {
            pool.remove(it)
        }
        // Don't clear attachment here because a file can be used in many BinaryAttachment
    }

    /**
     * Utility method to find an unused key in the pool
     */
    abstract fun findUnusedKey(): T

    /**
     * Return key of [binaryFileToRetrieve] or null if not found
     */
    private fun findKey(binaryFileToRetrieve: BinaryFile): T? {
        val contains = pool.containsValue(binaryFileToRetrieve)
        return if (!contains)
            null
        else {
            for ((key, binary) in pool) {
                if (binary == binaryFileToRetrieve) {
                    return key
                }
            }
            return null
        }
    }

    /**
     * To do an action on each binary in the pool
     */
    fun doForEachBinary(action: (key: T, binary: BinaryFile) -> Unit) {
        for ((key, value) in pool) {
            action.invoke(key, value)
        }
    }

    fun isEmpty(): Boolean {
        return pool.isEmpty()
    }

    @Throws(IOException::class)
    fun clear() {
        doForEachBinary { _, binary ->
            binary.clear()
        }
        pool.clear()
    }

    override fun toString(): String {
        val stringBuffer = StringBuffer()
        for ((key, value) in pool) {
            if (stringBuffer.isNotEmpty())
                stringBuffer.append(", {$key:$value}")
            else
                stringBuffer.append("{$key:$value}")
        }
        return stringBuffer.toString()
    }

    /**
     * Utility class to order binaries
     */
    class KeyBinary<T>(val binary: BinaryFile, key: T) {
        val keys = HashSet<T>()
        init {
            addKey(key)
        }

        fun addKey(key: T) {
            keys.add(key)
        }
    }
}
