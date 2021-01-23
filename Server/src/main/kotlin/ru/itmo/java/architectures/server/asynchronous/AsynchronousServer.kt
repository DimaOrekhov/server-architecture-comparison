package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.mean
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors

class AsynchronousServer(poolSize: Int) : TimedServer {

    private val serverSocketChannel = AsynchronousServerSocketChannel.open()

    private val taskPool = Executors.newFixedThreadPool(poolSize)
    private val clients = ConcurrentLinkedDeque<AsynchronousClientWorker>()

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

    @Volatile
    private var computedMetrics = false

    override fun reset() {
        clients.forEach { it.close() }
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

    override fun start() = serverSocketChannel
            .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
            .let { serverSocket -> acceptLoop(serverSocket) }

    private fun acceptLoop(serverSocket: AsynchronousServerSocketChannel) =
        serverSocket.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
            override fun completed(result: AsynchronousSocketChannel?, attachment: Nothing?) {
                val client = AsynchronousClientWorker(result!!, taskPool, System.currentTimeMillis())
                clients.add(client)
                client.start()
                serverSocket.accept(null, this)
            }

            override fun failed(exc: Throwable?, attachment: Nothing?) {
                serverSocketChannel.close()
            }
        })

    override fun shutdown() {
        clients.forEach { it.close() }
        serverSocketChannel.close()
    }
}