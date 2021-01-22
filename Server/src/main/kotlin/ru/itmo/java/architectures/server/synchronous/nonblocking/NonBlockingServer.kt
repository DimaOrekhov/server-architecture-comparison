package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.whileNotInterrupted
import ru.itmo.java.architectures.server.domain.TimedServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

class NonBlockingServer(poolSize: Int) : TimedServer {

    companion object {
        const val INPUT_SELECT_TIMEOUT_MS = 3000L

        fun <T> MutableIterator<T>.processAndRemoveEach(body: (T) -> Unit) {
            while (hasNext()) {
                body(next())
                // TODO: find out whether removed keys get any updates
                remove()
            }
        }
    }

    private val inputSelector = Selector.open()
    private val acceptPool = Executors.newSingleThreadExecutor()
    private val inputSelectorPool = Executors.newSingleThreadExecutor()
    private val outputSelectorPool = Executors.newSingleThreadExecutor()
    private val taskPool = Executors.newFixedThreadPool(poolSize)

    override fun start() {
        acceptPool.submit {
            ServerSocketChannel.open()
                    .bind(InetSocketAddress(Constants.SERVER_ADDRESS, Constants.SERVER_PORT))
                    .use { serverSocketChannel -> acceptLoop(serverSocketChannel) }
        }

        inputSelectorPool.submit { inputSelectorLoop() }

        outputSelectorPool.submit {

        }
    }

    private fun acceptLoop(serverSocketChannel: ServerSocketChannel) = whileNotInterrupted {
        val socketChannel = serverSocketChannel.accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(inputSelector, SelectionKey.OP_READ, NonBlockingClientWorker(taskPool))
    }

    private fun inputSelectorLoop() = whileNotInterrupted {
        inputSelector.select(INPUT_SELECT_TIMEOUT_MS)
        inputSelector.selectedKeys().iterator().processAndRemoveEach { selectionKey ->
            if (!selectionKey.isReadable) {
                return@processAndRemoveEach
            }

            val client = selectionKey.attachment() as NonBlockingClientWorker
            if (!client.isAvailableForRead.compareAndSet(true, true)) {
                return@processAndRemoveEach
            }

            val clientBuffer = client.currentBuffer ?: return@processAndRemoveEach
            val channel = selectionKey.channel() as SocketChannel
            channel.read(clientBuffer)
            client.maybeSubmitTask()
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