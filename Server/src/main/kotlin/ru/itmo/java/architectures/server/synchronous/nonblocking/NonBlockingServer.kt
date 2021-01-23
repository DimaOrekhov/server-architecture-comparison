package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.whileNotInterrupted
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class NonBlockingServer(poolSize: Int) : TimedServer {

    companion object {
        const val INPUT_SELECT_TIMEOUT_MS = 500L

        fun <T> MutableIterator<T>.processAndRemoveEach(body: (T) -> Unit) {
            while (hasNext()) {
                body(next())
                // TODO: find out whether removed keys get any updates
                remove()
            }
        }
    }

    private val inputSelector = Selector.open()
    private val outputSelector = Selector.open()

    private val acceptPool = Executors.newSingleThreadExecutor()
    private val inputSelectorPool = Executors.newSingleThreadExecutor()
    private val outputSelectorPool = Executors.newSingleThreadExecutor()
    private val taskPool = Executors.newFixedThreadPool(poolSize)

    private val clients = Collections.newSetFromMap(ConcurrentHashMap<NonBlockingClientProcessor, Boolean>())

    override val meanRequestResponseTimeMs: Long
        get() = TODO("Not yet implemented")
    override val meanTaskTimeMs: Long
        get() = TODO("Not yet implemented")

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun start() {
        acceptPool.submit {
            ServerSocketChannel.open()
                    .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
                    .use { serverSocketChannel -> acceptLoop(serverSocketChannel) }
        }

        // TODO: add some wait mechanism for first client?
        inputSelectorPool.submit { inputSelectorLoop() }

        outputSelectorPool.submit { outputSelectorLoop() }
    }

    private fun acceptLoop(serverSocketChannel: ServerSocketChannel) = whileNotInterrupted {
        val socketChannel = serverSocketChannel.accept()
        socketChannel.configureBlocking(false)

        val client = NonBlockingClientProcessor(socketChannel, taskPool)
        clients.add(client)
        socketChannel.register(inputSelector, SelectionKey.OP_READ, client.inputProcessor)
    }

    private fun inputSelectorLoop() = inputSelector.use { inputSelector ->
        whileNotInterrupted {
            inputSelector.select(INPUT_SELECT_TIMEOUT_MS)
            inputSelector.selectedKeys().iterator().processAndRemoveEach { selectionKey ->
                if (!selectionKey.isReadable) {
                    return@processAndRemoveEach
                }

                val client = selectionKey.attachment() as NonBlockingClientInputProcessor
                val channel = selectionKey.channel() as SocketChannel
                val clientBuffer = client.currentBuffer

                channel.read(clientBuffer)
                client.maybeSubmitTask()
            }
        }
    }

    private fun outputSelectorLoop() = outputSelector.use { outputSelector ->
        whileNotInterrupted {
            outputSelector.select()
            outputSelector.selectedKeys().iterator().processAndRemoveEach { selectionKey ->
                if (!selectionKey.isWritable) {
                    return@processAndRemoveEach
                }

                val client = selectionKey.attachment()
                val channel = selectionKey.channel() as SocketChannel

                //channel.write()
            }

            updateRegistered()
        }
    }

    private fun updateRegistered() = clients.forEach { client ->
        val channel = client.channel
        when (client.channelState) {
            ChannelState.NEW -> {
                channel.register(outputSelector, SelectionKey.OP_WRITE, client)

                client.channelState = ChannelState.REGISTERED
            }
            ChannelState.REGISTERED -> {
                if (client.responseQueue.isNotEmpty()) {
                    return@forEach
                }

                val selectionKey = channel.keyFor(outputSelector)
                selectionKey.cancel()

                client.channelState = ChannelState.DEREGISTERED
            }
            ChannelState.DEREGISTERED -> {
                if (client.responseQueue.isEmpty()) {
                    return@forEach
                }

                channel.register(outputSelector, SelectionKey.OP_WRITE, client)

                client.channelState = ChannelState.REGISTERED
            }
        }
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }
}