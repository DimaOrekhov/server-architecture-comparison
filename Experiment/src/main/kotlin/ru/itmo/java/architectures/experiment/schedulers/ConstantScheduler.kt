package ru.itmo.java.architectures.experiment.schedulers

class ConstantScheduler<T>(private val constant: T) : Scheduler<T> {
    override fun iterator(): Iterator<T> = generateSequence { constant }.iterator()
}
