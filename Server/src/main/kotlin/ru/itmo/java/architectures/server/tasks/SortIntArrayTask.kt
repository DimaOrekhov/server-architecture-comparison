package ru.itmo.java.architectures.server.tasks

import java.util.concurrent.Callable

abstract class SortingIntArrayTask(val elements: IntArray, val inPlace: Boolean = false) : Callable<IntArray> {
    override fun call(): IntArray = sort()

    protected abstract fun sort(): IntArray
}

class InsertionSortIntArrayTask(elements: IntArray, inPlace: Boolean = false) : SortingIntArrayTask(elements, inPlace) {
    override fun sort(): IntArray {
        val sortedCopy = if (inPlace) elements else elements.copyOf()
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
