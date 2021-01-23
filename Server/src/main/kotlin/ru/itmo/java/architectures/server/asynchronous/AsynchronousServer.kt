package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AsynchronousServer(poolSize: Int) : TimedServer {

    private val channelGroup = AsynchronousChannelGroup.withFixedThreadPool(poolSize) { runnable -> Thread(runnable) }
    private val serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup)

    override val meanRequestResponseTimeMs: Long
        get() = TODO("Not yet implemented")
    override val meanTaskTimeMs: Long
        get() = TODO("Not yet implemented")

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun start() = serverSocketChannel
            .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
            .let { serverSocket -> acceptLoop(serverSocket) }

    private fun acceptLoop(serverSocket: AsynchronousServerSocketChannel) =
        serverSocket.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
            override fun completed(result: AsynchronousSocketChannel?, attachment: Nothing?) {
                AsynchronousClientWorker(result!!, System.currentTimeMillis()) {
                    //TODO("ADD TIME PROCESSING CALLBACK")
                }.start()
                serverSocket.accept(null, this)
            }

            override fun failed(exc: Throwable?, attachment: Nothing?) {
                serverSocketChannel.close()
            }
        })

    override fun shutdown() {
        TODO("Not yet implemented")
    }
}