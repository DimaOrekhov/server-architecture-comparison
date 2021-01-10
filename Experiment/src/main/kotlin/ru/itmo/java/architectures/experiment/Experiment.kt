package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.client.ClientPool
import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.Scheduler
import ru.itmo.java.architectures.server.synchronous.SynchronousBlockingServer

class Experiment(private val config: ExperimentConfig) {

    companion object {
        private fun <T> Scheduler<T>.isNotConstant() = this !is ConstantScheduler
    }

    fun run() {
        // TODO: validate that only one scheduler is not constant
        val nRequests = config.nRequestsScheduler.iterator().next()
        val nClients = config.nClientsScheduler.iterator().next()
        val nElements = config.nElementsScheduler.iterator().next()
        val requestDelayMs = config.requestDelayMsScheduler.iterator().next()
        for (nClients in config.nClientsScheduler) {
            step(nRequests = nRequests, nClients = nClients, nElements = nElements, requestDelayMs = requestDelayMs)
        }
    }

    private fun step(nRequests: Int, nClients: Int, nElements: Int, requestDelayMs: Long) {
        val server = when (config.architectureType) {
            ServerArchitectureType.SYNCHRONOUS_BLOCKING -> SynchronousBlockingServer(nClients) // TODO: remove this parameter at all
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
    }

}
