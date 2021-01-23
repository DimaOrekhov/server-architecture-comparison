package ru.itmo.java.architectures.server.synchronous.blocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.mean
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class SynchronousBlockingServer(poolSize: Int) : TimedServer {

    private val acceptPool = Executors.newSingleThreadExecutor()
    private val globalThreadPool = Executors.newFixedThreadPool(poolSize)
    private val clients = mutableListOf<SynchronousBlockingClientWorker>()

    @Volatile
    private var computedMetrics = false
    private var currentMeanRequestResponseTimeMs: Double = 0.0
    private var currentMeanTaskTimeMs: Double = 0.0

    override val meanRequestResponseTimeMs: Double
        get() {
            computeMetrics()
            return currentMeanRequestResponseTimeMs
        }
    override val meanTaskTimeMs: Double
        get() {
            computeMetrics()
            return currentMeanTaskTimeMs
        }

    override fun reset() {
        clients.forEach { it.close() }
        clients.clear()
        computedMetrics = false
    }

    private fun computeMetrics() {
        if (computedMetrics) {
            return
        }
        currentMeanRequestResponseTimeMs = clients.map {it.meanRequestResponseTimeMs}.mean()
        currentMeanTaskTimeMs = clients.map {it.meanTaskTimeMs}.mean()
        computedMetrics = true
    }

    override fun start() {
        acceptPool.submit {
            ServerSocket(
                Constants.SERVER_PORT,
                Constants.SERVER_BACKLOG,
                InetAddress.getByName(Constants.SERVER_ADDRESS)
            ).use { serverSocket ->
                while (!Thread.interrupted()) {
                    val socket = serverSocket.accept()
                    createClient(socket)
                }
            }
        }
    }

    override fun shutdown() {
        acceptPool.shutdown()
        clients.forEach { it.close() }
        globalThreadPool.shutdown()
    }

    private fun createClient(socket: Socket) {
        val clientWorker = SynchronousBlockingClientWorker(socket, globalThreadPool)
        clients.add(clientWorker)
        clientWorker.start()
    }
}
