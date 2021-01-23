package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class AsynchronousServer(poolSize: Int) : TimedServer() {

    private val serverSocketChannel = AsynchronousServerSocketChannel.open()

    private val taskPool = Executors.newFixedThreadPool(poolSize)
    override val clients: MutableCollection<AsynchronousClientWorker> =
            Collections.newSetFromMap(ConcurrentHashMap())

    override fun start() = serverSocketChannel
            .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
            .let { serverSocket -> acceptLoop(serverSocket) }

    private fun acceptLoop(serverSocket: AsynchronousServerSocketChannel) =
        serverSocket.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
            override fun completed(result: AsynchronousSocketChannel?, attachment: Nothing?) {
                val client = AsynchronousClientWorker(result!!, taskPool)
                clients.add(client)
                client.start()
                serverSocket.accept(null, this)
            }

            override fun failed(exc: Throwable?, attachment: Nothing?) {
                serverSocketChannel.close()
            }
        })

    override fun shutdown() {
        clients.forEach { it.shutdown() }
        serverSocketChannel.close()
    }
}