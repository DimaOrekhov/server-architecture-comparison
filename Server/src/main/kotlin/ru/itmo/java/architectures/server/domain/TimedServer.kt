package ru.itmo.java.architectures.server.domain

import ru.itmo.java.architectures.common.Utils.mean

abstract class TimedServer: Server {
    protected abstract val clients: MutableCollection<out ClientWorker>

    @Volatile
    private var computedMetrics = false
    private var currentMeanRequestResponseTimeMs: Double = 0.0
    private var currentMeanTaskTimeMs: Double = 0.0

    val meanRequestResponseTimeMs: Double
        get() {
            computeMetrics()
            return currentMeanRequestResponseTimeMs
        }
    val meanTaskTimeMs: Double
        get() {
            computeMetrics()
            return currentMeanTaskTimeMs
        }

    fun reset() {
        clients.forEach { it.shutdown() }
        clients.clear()
        computedMetrics = false
    }

    private fun computeMetrics() {
        if (computedMetrics) {
            return
        }
        currentMeanRequestResponseTimeMs = clients.map { it.meanRequestResponseTimeMs }.mean()
        currentMeanTaskTimeMs = clients.map { it.meanTaskTimeMs }.mean()
    }
}
