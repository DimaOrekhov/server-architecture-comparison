package ru.itmo.java.architectures.experiment.schedulers

import ru.itmo.java.architectures.experiment.schedulers.NumberUtils.compareTo
import ru.itmo.java.architectures.experiment.schedulers.NumberUtils.plus

class LinearScheduler<T: Number>(private val start: T, private val exclusiveEnd: T, private val step: T) : Scheduler<T> {
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): Iterator<T> =
            sequence {
                var current = start
                if (start < exclusiveEnd) {
                    while (start < exclusiveEnd) {
                        yield(current)
                        current = (current + step) as T
                    }
                } else {
                    while (start > exclusiveEnd) {
                        yield(current)
                        current = (current + step) as T
                    }
                }
            }.iterator()
}
