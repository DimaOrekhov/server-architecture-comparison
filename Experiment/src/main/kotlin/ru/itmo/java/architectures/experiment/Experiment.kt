package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.client.ClientPool
import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.Scheduler
import ru.itmo.java.architectures.server.synchronous.SynchronousBlockingServer
import java.lang.IllegalArgumentException

class Experiment(private val config: ExperimentConfig) {

    companion object {
        private fun <T> Scheduler<T>.isNotConstant() = this !is ConstantScheduler

        private fun ExperimentConfig.getSchedulers() =
                listOf(nRequestsScheduler, nClientsScheduler, nElementsScheduler, requestDelayMsScheduler)
    }

    fun run() {
        validateConfig()
        config.iterateOver()
    }

    private fun validateConfig() {
        val numberOfNonConstantSchedulers: Int = config.getSchedulers()
                .sumBy { if (it.isNotConstant()) 1 else 0 }

        when (numberOfNonConstantSchedulers) {
            0 -> throw IllegalArgumentException("Specify parameter to vary")
            1 -> throw IllegalArgumentException("Experiment configuration with multiple varying parameters is not supported")
        }
    }

    private fun ExperimentConfig.iterateOver() {
        val nRequests = nRequestsScheduler.iterator().next()
        val nClients = nClientsScheduler.iterator().next()
        val nElements = nElementsScheduler.iterator().next()
        val requestDelayMs = requestDelayMsScheduler.iterator().next()

        when {
            nRequestsScheduler.isNotConstant() -> nRequestsScheduler.forEach {
                step(nRequests = it, nClients = nClients, nElements = nElements, requestDelayMs = requestDelayMs)
            }
            nClientsScheduler.isNotConstant() -> nClientsScheduler.forEach {
                step(nRequests = nRequests, nClients = it, nElements = nElements, requestDelayMs = requestDelayMs)
            }
            nElementsScheduler.isNotConstant() -> nElementsScheduler.forEach {
                step(nRequests = nRequests, nClients = nClients, nElements = it, requestDelayMs =requestDelayMs)
            }
            requestDelayMsScheduler.isNotConstant() -> requestDelayMsScheduler.forEach {
                step(nRequests = nRequests, nClients = nClients, nElements = nElements, requestDelayMs = it)
            }
        }
    }

    private fun step(nRequests: Int, nClients: Int, nElements: Int, requestDelayMs: Long) {
        val server = when (config.architectureType) {
            ServerArchitectureType.SYNCHRONOUS_BLOCKING -> SynchronousBlockingServer(nClients) // TODO: remove this parameter (pool size) at all
            ServerArchitectureType.ASYNCHRONOUS -> null
            ServerArchitectureType.NONBLOCKING -> null
        }
        val clientPool = ClientPool(
                address = Constants.SERVER_ADDRESS,
                port = Constants.SERVER_PORT,
                nClients = nClients,
                nRequests = nRequests,
                nElements = nElements,
                requestDelay = requestDelayMs
        )
        // TODO: save results
    }

}
