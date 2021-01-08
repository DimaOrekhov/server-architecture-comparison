package ru.itmo.java.architectures.server.tasks

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextInt

abstract class SortTest {

    companion object {
        private const val N_STRESS_TEST_STEPS = 100
        private val randomArraySizeRange = IntRange(1, 10_000)
    }

    protected abstract fun <V: Comparable<V>> sort(array: Array<V>): Array<V>

    @Test
    fun emptyArraySortTest() {
        val array = Array(0) { 0 }
        assertArrayEquals(array, sort(array))
    }

    @Test
    fun sortedArraySortTest() {
        val array = Array(100) { it }
        assertArrayEquals(array, sort(array))
    }

    @Test
    fun simpleTest() {
        val array = arrayOf(2, -10, 3, 6, 3)
        assertArrayEquals(arrayOf(-10, 2, 3, 3, 6), sort(array))
    }

    @Test
    fun randomArraySortTest() {
        val arraySize = Random.nextInt(randomArraySizeRange)
        val array = Array(arraySize) { Random.nextInt() }
        assertArrayEquals(array.sortedArray(), sort(array))
    }

    @Test
    fun randomArraySortStressTest() = (1 .. N_STRESS_TEST_STEPS).forEach { _ -> randomArraySortTest() }
}
