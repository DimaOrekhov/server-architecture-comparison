package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
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

    companion object {
        fun <A> channelClosedPredicate(result: Int, attachment: A) = result == -1
        fun <A> channelClosedPredicate(result: Long, attachment: A) = result == -1L
    }

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
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

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
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

    private val sendResponseHandler = object : CompletionHandler<Int, Array<ByteBuffer>> {
        override fun completed(result: Int?, attachment: Array<ByteBuffer>?) {
            val responseBuffers = attachment!!
            when {
                responseBuffers[0].hasRemaining() -> channel.write(responseBuffers[0], responseBuffers, this)
                responseBuffers[1].hasRemaining() -> channel.write(responseBuffers[1], responseBuffers, this)
                else -> readHeader()
            }
        }

        override fun failed(exc: Throwable?, attachment: Array<ByteBuffer>?) {
            TODO("Not yet implemented")
        }
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

    fun start() = readHeader()

    private fun readHeader() {
        val headerBuffer = ByteBuffer.allocate(Constants.HEADER_SIZE)
        channel.read(headerBuffer, headerBuffer, headerHandler)
    }

    override fun close() {
        totalTime = System.currentTimeMillis() - startTime
        onClose()
    }
}
