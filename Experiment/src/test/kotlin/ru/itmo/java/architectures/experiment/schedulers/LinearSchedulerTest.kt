package ru.itmo.java.architectures.experiment.schedulers

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.absoluteValue

class LinearSchedulerTest {

    companion object {
        private val longStart = 10_000_000_000L
        private val longExclusiveEnd = 10_000_001_000L
    }

    private fun linearIntTest(start: Int, exclusiveEnd: Int, step: Int) {
        val scheduler = LinearScheduler(start, exclusiveEnd, step)
        val rangeStep = step.absoluteValue
        val range = (if (start < exclusiveEnd) (start until exclusiveEnd) else (start downTo exclusiveEnd + 1)) step rangeStep
        val rangeIterator = range.iterator()
        for (v in scheduler) {
            assertEquals(rangeIterator.next(), v)
        }
    }

    private fun linearLongTest(start: Long, exclusiveEnd: Long, step: Long) {
        val scheduler = LinearScheduler(start, exclusiveEnd, step)
        val rangeStep = step.absoluteValue
        val range = (if (start < exclusiveEnd) (start until exclusiveEnd) else (start downTo exclusiveEnd + 1)) step rangeStep
        val rangeIterator = range.iterator()
        for (v in scheduler) {
            assertEquals(rangeIterator.next(), v)
        }
    }

    @Test
    fun incrementalGrowthIntTest() = linearIntTest(-10, 50, 1)

    @Test
    fun incrementalGrowthLongTest() = linearLongTest(longStart, longExclusiveEnd, 1)

    @Test
    fun linearGrowthIntTest() = linearIntTest(-10, 50, 3)

    @Test
    fun linearGrowthLongTest() = linearLongTest(longStart, longExclusiveEnd, 3)

    @Test
    fun decrementalDecayIntTest() = linearIntTest(50, -10, -1)

    @Test
    fun decrementalDecayLongTest() = linearLongTest(longExclusiveEnd, longStart, -1)

    @Test
    fun linearDecayIntTest() = linearIntTest(50, -10, -3)

    @Test
    fun linearDecayLongTest() = linearLongTest(longExclusiveEnd, longStart, -3)

    @Test
    fun emptyIntTest() = linearLongTest(1, 1, 1)

    @Test
    fun singleValueIntTest() = linearLongTest(1, 2, 4)
}
