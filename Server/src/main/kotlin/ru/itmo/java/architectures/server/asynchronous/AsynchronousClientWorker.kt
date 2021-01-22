package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Utils.toBuffersArray
import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.ClientWorker
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.asTimed
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AsynchronousClientWorker(private val channel: AsynchronousSocketChannel,
                               private val startTime: Long,
                               private val onClose: AsynchronousClientWorker.() -> Unit = {}) : ClientWorker {

    @Volatile
    var totalTime: Long = 0
        private set
    @Volatile
    var taskTime: Long = 0
        private set

    private val headerHandler = object : CompletionHandler<Int, ByteBuffer> {
        override fun completed(result: Int?, attachment: ByteBuffer?) {
            val buffer = attachment!!
            if (buffer.hasRemaining()) {
                // Continue reading header
                channel.read(buffer, buffer, this)
                return
            }

            buffer.flip()
            val bodySize = buffer.int
            val bodyBuffer = ByteBuffer.allocate(bodySize)
            channel.read(bodyBuffer, bodyBuffer, bodyReadHandler)
        }

        override fun failed(exc: Throwable?, attachment: ByteBuffer?) {
            TODO("Not yet implemented")
        }
    }

    private val bodyReadHandler = object : CompletionHandler<Int, ByteBuffer> {
        override fun completed(result: Int?, attachment: ByteBuffer?) {
            val buffer = attachment!!
            if (buffer.hasRemaining()) {
                channel.read(buffer, buffer, this)
                return
            }

            buffer.flip()
            val request = IntArrayMessage.parseFrom(buffer)
            val (sortedArray, taskTime) = SortCallable(request.elementsList.toTypedArray())
                    .asTimed()
                    .call()
            this@AsynchronousClientWorker.taskTime = taskTime
            val response = IntArrayMessage.newBuilder()
                    .addAllElements(sortedArray.toList())
                    .build()

            val responseBuffers = response.toBuffersArray()
            channel.write(responseBuffers[0], responseBuffers, sendResponseHandler)
        }

        override fun failed(exc: Throwable?, attachment: ByteBuffer?) {
            TODO("Not yet implemented")
        }
    }

    private val sendResponseHandler = object : CompletionHandler<Int, Array<ByteBuffer>> {
        override fun completed(result: Int?, attachment: Array<ByteBuffer>?) {
            val responseBuffers = attachment!!
            val currentBuffer = when {
                responseBuffers[0].hasRemaining() -> responseBuffers[0]
                responseBuffers[1].hasRemaining() -> responseBuffers[1]
                else -> {
                    close()
                    return
                }
            }

            channel.write(currentBuffer, responseBuffers, this)
        }

        override fun failed(exc: Throwable?, attachment: Array<ByteBuffer>?) {
            TODO("Not yet implemented")
        }
    }

    fun start() {
        val headerBuffer = ByteBuffer.allocate(4) // TODO: Move header size somewhere to Commons mb
        channel.read(headerBuffer, headerBuffer, headerHandler)
    }

    override fun close() {
        totalTime = System.currentTimeMillis() - startTime
        onClose()
    }
}
