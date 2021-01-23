package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.experiment.schedulers.Scheduler

data class ExperimentConfig(
        val architectureType: ServerArchitectureType,
        val nRequestsScheduler: Scheduler<Int>,
        val nClientsScheduler: Scheduler<Int>,
        val nElementsScheduler: Scheduler<Int>,
        val requestDelayMsScheduler: Scheduler<Long>)
