package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.LinearScheduler
import ru.itmo.java.architectures.experiment.schedulers.Scheduler

object FileUtils {
    const val YAML_SINGLE_OFFSET = 4

    fun <T> Scheduler<T>.describe(): String =
            when (this) {
                is ConstantScheduler -> constant.toString()
                is LinearScheduler -> "\"from $start to $exclusiveEnd (exclusively) with a step of $step\""
                else -> toString()
            }

    fun ExperimentConfig.asYaml(): String =
            listOf(
                    "Architecture kind: \"${architectureType.showName}\"",
                    "Number of clients: ${nClientsScheduler.describe()}",
                    "Number of requests per client: ${nRequestsScheduler.describe()}",
                    "Number of elements in array: ${nElementsScheduler.describe()}",
                    "Delay between requests ms: ${requestDelayMsScheduler.describe()}"
            ).joinToString("\n")

    fun ExperimentStepResult.asYamlEntry(offset: Int): String = listOf(
            "-",
            "meanServerSideRequestResponseTimeMs: $meanServerSideRequestResponseTimeMs",
            "meanServerSideTaskTimeMs: $meanServerSideTaskTimeMs",
            "meanClientSideRequestResponseTimeMs: $meanClientSideRequestResponseTimeMs"
    ).mapIndexed { i, seq ->
        val desiredLength = seq.length + if (i == 0) offset else offset + FileUtils.YAML_SINGLE_OFFSET
        seq.padStart(desiredLength)
    }.joinToString("\n")

    fun ExperimentResult.asYaml(): String = stepResults
            .joinToString("\n", "${config.asYaml()}\nresult:\n") { it.asYamlEntry(FileUtils.YAML_SINGLE_OFFSET) }
}
