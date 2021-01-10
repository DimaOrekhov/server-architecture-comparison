package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.LinearScheduler
import ru.itmo.java.architectures.experiment.schedulers.Scheduler

data class ExperimentConfig(
        val architectureType: ServerArchitectureType,
        val nRequestsScheduler: Scheduler<Int>,
        val nClientsScheduler: Scheduler<Int>,
        val nElementsScheduler: Scheduler<Int>,
        val requestDelayMsScheduler: Scheduler<Long>) {

    companion object {
        fun <T> Scheduler<T>.describe(): String =
                when (this) {
                    is ConstantScheduler -> constant.toString()
                    is LinearScheduler -> "from $start to $exclusiveEnd (exclusively) with a step of $step"
                    else -> toString()
                }
    }

    override fun toString(): String =
            listOf(
                    "Architecture kind: ${architectureType.showName}",
                    "Number of clients: ${nClientsScheduler.describe()}",
                    "Number of requests per client: ${nRequestsScheduler.describe()}",
                    "Thread pool size: ${nElementsScheduler.describe()}",
                    "Delay between requests ms: ${requestDelayMsScheduler.describe()}"
            ).joinToString("\n")
}
