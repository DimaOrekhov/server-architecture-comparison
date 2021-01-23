package ru.itmo.java.architectures.server.synchronous.blocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class SynchronousBlockingServer(poolSize: Int) : TimedServer() {

    private val acceptPool = Executors.newSingleThreadExecutor()
    private val globalThreadPool = Executors.newFixedThreadPool(poolSize)
    override val clients: MutableCollection<SynchronousBlockingClientWorker> = mutableListOf()

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
        clients.forEach { it.shutdown() }
        globalThreadPool.shutdown()
    }

    private fun createClient(socket: Socket) {
        val clientWorker = SynchronousBlockingClientWorker(socket, globalThreadPool)
        clients.add(clientWorker)
        clientWorker.start()
    }
}
