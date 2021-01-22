package ru.itmo.java.architectures.server.tasks

import org.junit.Assert
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextInt

abstract class SortIntArrayTest {
    companion object {
        private const val N_STRESS_TEST_STEPS = 100
        private val randomArraySizeRange = IntRange(1, 10_000)
    }

    protected abstract fun sort(array: IntArray): IntArray

    protected abstract fun inPlaceSort(array: IntArray): IntArray

    @Test
    fun emptyArraySortTest() {
        val array = IntArray(0) { 0 }
        Assert.assertArrayEquals(array, sort(array))
    }

    @Test
    fun sortedArraySortTest() {
        val array = IntArray(100) { it }
        Assert.assertArrayEquals(array, sort(array))
    }

    @Test
    fun simpleTest() {
        val array = intArrayOf(2, -10, 3, 6, 3)
        Assert.assertArrayEquals(intArrayOf(-10, 2, 3, 3, 6), sort(array))
    }

    @Test
    fun simpleInPlaceTest() {
        val array = intArrayOf(2, -10, 3, 6, 3)
        inPlaceSort(array)
        Assert.assertArrayEquals(intArrayOf(-10, 2, 3, 3, 6), array)
    }

    @Test
    fun randomArraySortTest() {
        val arraySize = Random.nextInt(randomArraySizeRange)
        val array = IntArray(arraySize) { Random.nextInt() }
        Assert.assertArrayEquals(array.sortedArray(), sort(array))
    }

    @Test
    fun randomArrayInPlaceSortTest() {
        val arraySize = Random.nextInt(randomArraySizeRange)
        val array = IntArray(arraySize) { Random.nextInt() }
        val sortedCopy = array.copyOf()
        inPlaceSort(sortedCopy)
        Assert.assertArrayEquals(array.sortedArray(), sortedCopy)
    }

    @Test
    fun randomArraySortStressTest() = (1 .. N_STRESS_TEST_STEPS).forEach { _ -> randomArraySortTest() }

    @Test
    fun randomArrayInPlaceSortStressTest() = (1 .. N_STRESS_TEST_STEPS).forEach { _ -> randomArrayInPlaceSortTest() }
}