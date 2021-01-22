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
    private var foo: Long = 0

    override fun start() {
        val serverSocket = AsynchronousServerSocketChannel.open(channelGroup)
                .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
        while (!Thread.interrupted()) {
            serverSocket.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
                override fun completed(result: AsynchronousSocketChannel?, attachment: Nothing?) {
                    val worker = AsynchronousClientWorker(result!!, System.currentTimeMillis())
                }

                override fun failed(exc: Throwable?, attachment: Nothing?) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    override fun getJobExecutionTimesMs(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun getRequestResponseTimesMs(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}