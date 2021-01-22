package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.whileNotInterrupted
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AsynchronousServer(poolSize: Int) : TimedServer {

    private val channelGroup = AsynchronousChannelGroup.withFixedThreadPool(poolSize) { runnable -> Thread(runnable) }

    override val meanRequestResponseTimeMs: Long
        get() = TODO("Not yet implemented")
    override val meanTaskTimeMs: Long
        get() = TODO("Not yet implemented")

    override fun resetMeasurements() {
        TODO("Not yet implemented")
    }

    override fun start() = AsynchronousServerSocketChannel.open(channelGroup)
            .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
            .use { serverSocket -> acceptLoop(serverSocket) }

    private fun acceptLoop(serverSocket: AsynchronousServerSocketChannel) = whileNotInterrupted {
        serverSocket.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
            override fun completed(result: AsynchronousSocketChannel?, attachment: Nothing?) {
                AsynchronousClientWorker(result!!, System.currentTimeMillis()) {
                    TODO("ADD TIME PROCESSING CALLBACK")
                }.start()
            }

            override fun failed(exc: Throwable?, attachment: Nothing?) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun close() {
        TODO("Time processing")
    }
}