package ru.itmo.java.architectures.server.tasks

import java.util.concurrent.Callable

open class TimedCallable<V>(private val callable: Callable<V>): Callable<TimedCallable.ResultWithTimeMeasurement<V>> {

    data class ResultWithTimeMeasurement<V>(val result: V, val timeMs: Long)

    override fun call(): ResultWithTimeMeasurement<V> {
        val startTime = System.currentTimeMillis()
        val result = callable.call()
        val elapsed = System.currentTimeMillis() - startTime
        return ResultWithTimeMeasurement(result, elapsed)
    }
}

fun <V> Callable<V>.asTimed(): TimedCallable<V> = TimedCallable(this)
