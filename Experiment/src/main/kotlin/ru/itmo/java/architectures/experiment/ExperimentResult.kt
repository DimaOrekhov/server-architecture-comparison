package ru.itmo.java.architectures.experiment

data class ExperimentResult(val config: ExperimentConfig, val stepResults: List<ExperimentStepResult>)

data class ExperimentStepResult(
        val meanServerSideRequestResponseTimeMs: Double,
        val meanServerSideTaskTimeMs: Double,
        val meanClientSideRequestResponseTimeMs: Double
)
