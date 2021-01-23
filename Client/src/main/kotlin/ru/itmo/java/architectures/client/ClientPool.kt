package ru.itmo.java.architectures.client

import ru.itmo.java.architectures.common.Utils.mean
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClientPool(
    address: String,
    port: Int,
    private val nClients: Int,
    private val nRequests: Int,
    private val nElements: Int,
    private val requestDelay: Long,
) : Runnable {

    @Volatile
    private var meanClientTime: Double = 0.0
    @Volatile
    private var isTerminated = false

    companion object {
        const val CLIENT_POOL_SIZE = 4
        const val AWAIT_STEP_MS = 1_000L
        const val MAX_AWAIT_TIME_MS = 30_000L
    }

    private val clientsThreadPool = Executors.newFixedThreadPool(CLIENT_POOL_SIZE)

    private val clients = Array(nClients) { Client(address, port, nRequests, nElements, requestDelay, this::signalFinished) }

    private val lock = ReentrantLock()
    private val isDone = lock.newCondition()
    private val finishedCounter = AtomicInteger(0)

    private fun signalFinished() = lock.withLock {
        if (finishedCounter.incrementAndGet() == nClients) {
            isDone.signal()
        }
    }

    private fun awaitTermination(): Boolean = lock.withLock {
        val startWaitTime = System.currentTimeMillis()
        while (finishedCounter.get() < nClients) {
            isDone.await(AWAIT_STEP_MS, TimeUnit.MILLISECONDS)

            val totalWaitTime = System.currentTimeMillis() - startWaitTime
            if (totalWaitTime > MAX_AWAIT_TIME_MS) {
                return false
            }
        }
        isTerminated = true
        return true
    }

    override fun run() = clients.forEach { clientsThreadPool.submit(it) }

    private fun computeMetrics() {
        // TODO: Check no client resulted in exception or smth like that
        meanClientTime = clients.filter { it.state == Client.ClientState.DONE }
                .map(Client::meanRequestResponseTime)
                .mean()
    }

    fun awaitTerminationAndGetMeanClientTime(): Double {
        if (!isTerminated) {
            awaitTermination()
        }
        computeMetrics()
        return meanClientTime
    }

    fun shutdown() = clientsThreadPool.shutdown()
}
