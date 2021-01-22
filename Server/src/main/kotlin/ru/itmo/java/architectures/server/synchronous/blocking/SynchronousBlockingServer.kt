package ru.itmo.java.architectures.server.synchronous.blocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class SynchronousBlockingServer(poolSize: Int) : TimedServer {

    private val acceptPool = Executors.newSingleThreadExecutor()
    private val globalThreadPool = Executors.newFixedThreadPool(poolSize)
    private val clients = mutableListOf<SynchronousBlockingClientWorker>()

    override val meanRequestResponseTimeMs: Long
        get() = TODO("Not yet implemented")
    override val meanTaskTimeMs: Long
        get() = TODO("Not yet implemented")

    override fun resetMeasurements() {
        TODO("Not yet implemented")
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

    override fun close() {
        acceptPool.shutdown()
        clients.forEach { it.close() }
        globalThreadPool.shutdown()
        // TODO: save time here
    }

    private fun createClient(socket: Socket) {
        val clientWorker = SynchronousBlockingClientWorker(socket, globalThreadPool)
        clients.add(clientWorker)
        clientWorker.start()
    }
}
