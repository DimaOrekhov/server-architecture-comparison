package ru.itmo.java.architectures.experiment

import ru.itmo.java.architectures.client.ClientPool
import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.Scheduler
import ru.itmo.java.architectures.server.asynchronous.AsynchronousServer
import ru.itmo.java.architectures.server.domain.TimedServer
import ru.itmo.java.architectures.server.synchronous.blocking.SynchronousBlockingServer
import ru.itmo.java.architectures.server.synchronous.nonblocking.NonBlockingServer

class Experiment(private val config: ExperimentConfig) {

    companion object {
        private fun <T> Scheduler<T>.isNotConstant() = this !is ConstantScheduler

        private fun ExperimentConfig.getSchedulers() =
                listOf(nRequestsScheduler, nClientsScheduler, nElementsScheduler, requestDelayMsScheduler)
    }

    private val server: TimedServer = when (config.architectureType) {
        ServerArchitectureType.SYNCHRONOUS_BLOCKING -> SynchronousBlockingServer(Constants.SERVER_POOL_SIZE)
        ServerArchitectureType.ASYNCHRONOUS -> AsynchronousServer(Constants.SERVER_POOL_SIZE)
        ServerArchitectureType.NONBLOCKING -> NonBlockingServer(Constants.SERVER_POOL_SIZE)
    }

    fun run(): ExperimentResult {
        server.start()
        validateConfig()
        val results = config.iterateOver()
        server.shutdown()
        return ExperimentResult(config, results)
    }

    private fun validateConfig() {
        val numberOfNonConstantSchedulers: Int = config.getSchedulers()
                .sumBy { if (it.isNotConstant()) 1 else 0 }

        when {
            numberOfNonConstantSchedulers == 0 ->
                throw IllegalArgumentException("Specify parameter to vary")
            numberOfNonConstantSchedulers > 1 ->
                throw IllegalArgumentException("Experiment configuration with multiple varying parameters is not supported")
        }
    }

    private fun ExperimentConfig.iterateOver(): List<ExperimentStepResult> {
        val nRequests = nRequestsScheduler.iterator().next()
        val nClients = nClientsScheduler.iterator().next()
        val nElements = nElementsScheduler.iterator().next()
        val requestDelayMs = requestDelayMsScheduler.iterator().next()

        return when {
            nRequestsScheduler.isNotConstant() -> nRequestsScheduler.map {
                step(nRequests = it, nClients = nClients, nElements = nElements, requestDelayMs = requestDelayMs)
            }
            nClientsScheduler.isNotConstant() -> nClientsScheduler.map {
                step(nRequests = nRequests, nClients = it, nElements = nElements, requestDelayMs = requestDelayMs)
            }
            nElementsScheduler.isNotConstant() -> nElementsScheduler.map {
                step(nRequests = nRequests, nClients = nClients, nElements = it, requestDelayMs =requestDelayMs)
            }
            requestDelayMsScheduler.isNotConstant() -> requestDelayMsScheduler.map {
                step(nRequests = nRequests, nClients = nClients, nElements = nElements, requestDelayMs = it)
            }
            else -> emptyList()
        }
    }

    private fun step(nRequests: Int, nClients: Int, nElements: Int, requestDelayMs: Long): ExperimentStepResult {
        val clientPool = ClientPool(
                address = Constants.SERVER_ADDRESS,
                port = Constants.SERVER_PORT,
                nClients = nClients,
                nRequests = nRequests,
                nElements = nElements,
                requestDelay = requestDelayMs
        )
        clientPool.run()

        val meanClientSideRequestResponseTimeMs = clientPool.awaitTerminationAndGetMeanClientTime()
        clientPool.shutdown()

        val meanServerSideTaskTimeMs = server.meanTaskTimeMs
        val meanServerSideRequestResponseTimeMs = server.meanRequestResponseTimeMs
        server.reset()

        return ExperimentStepResult(meanServerSideRequestResponseTimeMs = meanServerSideRequestResponseTimeMs,
                meanServerSideTaskTimeMs = meanServerSideTaskTimeMs,
                meanClientSideRequestResponseTimeMs = meanClientSideRequestResponseTimeMs)
    }

}
