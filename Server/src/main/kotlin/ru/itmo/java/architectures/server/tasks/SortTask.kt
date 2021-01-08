package ru.itmo.java.architectures.server.tasks

import java.util.concurrent.Callable

abstract class SortingTask<V: Comparable<V>>(protected val elements: Array<V>): Callable<Array<V>> {

    override fun call(): Array<V> = sort()

    protected abstract fun sort(): Array<V>
}

class InsertionSortTask<V: Comparable<V>>(elements: Array<V>): SortingTask<V> (elements) {
    override fun sort(): Array<V> {
        val sortedCopy = elements.copyOf()
        for (i in 1 until sortedCopy.size) {
            val currentElement = sortedCopy[i]
            var j = i - 1
            while (j >= 0 && sortedCopy[j] > currentElement) {
                sortedCopy[j+1] = sortedCopy[j]
                j--
            }
            sortedCopy[j+1] = currentElement
        }
        return sortedCopy
    }
}
