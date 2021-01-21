package ru.itmo.java.architectures.client

import ru.itmo.java.architectures.common.Utils.mean
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ClientPool(
    address: String,
    port: Int,
    nClients: Int,
    private val nRequests: Int,
    private val nElements: Int,
    private val requestDelay: Long,
) : Runnable {

    @Volatile
    private var meanClientTime: Double = 0.0
    @Volatile
    private var isTerminated = false

    companion object {
        const val TIMEOUT_S = 120L
    }

    private val clientsThreadPool = Executors.newFixedThreadPool(nClients)

    private val clients = Array(nClients) { Client(address, port, nRequests, nElements, requestDelay) }

    override fun run() = clients.forEach { clientsThreadPool.submit(it) }

    private fun awaitTermination(): Boolean {
        val poolTerminated = clientsThreadPool.awaitTermination(TIMEOUT_S, TimeUnit.SECONDS)
        if (!poolTerminated) {
            return false
        }
        // TODO: Check no client resulted in exception or smth like that
        clients.forEach { it.close() }
        // TODO: make in streams to avoid boxing
        meanClientTime = clients.filter(Client::isDone)
                .map(Client::runningTime)
                .mean()
        isTerminated = true
        return true
    }

    fun awaitTerminationAndGetMeanClientTime(): Double {
        if (!isTerminated) {
            awaitTermination()
        }
        return meanClientTime
    }
}
