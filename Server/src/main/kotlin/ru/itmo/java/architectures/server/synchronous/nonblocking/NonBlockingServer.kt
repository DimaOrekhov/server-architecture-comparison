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

class NonBlockingServer(poolSize: Int) : TimedServer() {

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

    private val serverSocket = ServerSocketChannel.open()
    private val inputSelector = Selector.open()
    private val outputSelector = Selector.open()

    private val acceptPool = Executors.newSingleThreadExecutor()
    private val inputSelectorPool = Executors.newSingleThreadExecutor()
    private val outputSelectorPool = Executors.newSingleThreadExecutor()
    private val taskPool = Executors.newFixedThreadPool(poolSize)

    override val clients: MutableCollection<NonBlockingClientWorker> =
            Collections.newSetFromMap(ConcurrentHashMap())

    override fun start() {
        acceptPool.submit {
            serverSocket.bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
                    .use { serverSocketChannel -> acceptLoop(serverSocketChannel) }
        }

        // TODO: add some wait mechanism for first client?
        inputSelectorPool.submit { inputSelectorLoop() }

        outputSelectorPool.submit { outputSelectorLoop() }
    }

    private fun acceptLoop(serverSocketChannel: ServerSocketChannel) = whileNotInterrupted {
        val socketChannel = serverSocketChannel.accept()
        socketChannel.configureBlocking(false)

        val client = NonBlockingClientWorker(socketChannel, outputSelector, taskPool)
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

                val client = selectionKey.attachment() as NonBlockingClientWorker
                val channel = selectionKey.channel() as SocketChannel
                val responseQueue = client.responseQueue

                if (responseQueue.isEmpty()) {
                    return@processAndRemoveEach
                }

                val response = responseQueue.peek()
                response.writeTo(channel)
                if (response.isEmpty()) {
                    responseQueue.poll()
                    client.markProcessed(response.id)
                }
            }

            updateRegistered()
        }
    }

    private fun updateRegistered() = clients.forEach { client ->
        val channel = client.channel
        when (client.channelState.get()) {
            ChannelState.NEW -> {
                channel.register(outputSelector, SelectionKey.OP_WRITE, client)

                client.channelState.set(ChannelState.REGISTERED)
            }
            ChannelState.REGISTERED -> {
                if (client.responseQueue.isNotEmpty()) {
                    return@forEach
                }

                val selectionKey = channel.keyFor(outputSelector)
                selectionKey.cancel()

                client.channelState.set(ChannelState.DEREGISTERED)
            }
        }
    }

    override fun shutdown() {
        acceptPool.shutdown()
        inputSelectorPool.shutdown()
        outputSelectorPool.shutdown()
        outputSelector.wakeup()
        inputSelector.wakeup()
        serverSocket.close()
        outputSelector.close()
        inputSelector.close()
    }
}
