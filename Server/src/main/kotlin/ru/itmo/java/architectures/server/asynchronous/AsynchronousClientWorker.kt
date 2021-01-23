package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.mean
import ru.itmo.java.architectures.common.Utils.toBuffersArray
import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.ClientWorker
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.Utils.thenApply
import ru.itmo.java.architectures.server.tasks.asTimed
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService

class AsynchronousClientWorker(private val channel: AsynchronousSocketChannel,
                               private val taskPool: ExecutorService,
                               private val startTime: Long,
                               private val onClose: AsynchronousClientWorker.() -> Unit = {}) : ClientWorker {

    companion object {
        fun <A> channelClosedPredicate(result: Int, attachment: A) = result == -1
    }

    override val meanRequestResponseTimeMs: Double
        get() = requestResponseListMs.mean()
    override val meanTaskTimeMs: Double
        get() = taskTimeListMs.mean()
    private val requestResponseListMs = ConcurrentLinkedDeque<Long>()
    private val taskTimeListMs = ConcurrentLinkedDeque<Long>()

    private val headerHandler = object : CompletionHandler<Int, AsynchronousClientAttachment> {
        override fun completed(result: Int?, attachment: AsynchronousClientAttachment?) {
            val buffer = attachment?.headerBuffer!!
            if (buffer.hasRemaining()) {
                channel.read(buffer, attachment, this)
                return
            }

            buffer.flip()
            val bodySize = buffer.int
            val bodyBuffer = ByteBuffer.allocate(bodySize)
            attachment.bodyBuffer = bodyBuffer
            channel.read(bodyBuffer, attachment, bodyReadHandler)
        }

        override fun failed(exc: Throwable?, attachment: AsynchronousClientAttachment?) {
            TODO("Not yet implemented")
        }
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

    private val bodyReadHandler = object : CompletionHandler<Int, AsynchronousClientAttachment> {
        override fun completed(result: Int?, attachment: AsynchronousClientAttachment?) {
            val buffer = attachment?.bodyBuffer!!
            if (buffer.hasRemaining()) {
                channel.read(buffer, attachment, this)
                return
            }

            buffer.flip()
            val request = IntArrayMessage.parseFrom(buffer)

            taskPool.submit(SortCallable(request.elementsList.toTypedArray())
                    .asTimed()
                    .thenApply { (sortedArray, taskTime) ->
                        attachment.taskTimeMs = taskTime
                        val response = IntArrayMessage.newBuilder()
                                .addAllElements(sortedArray.toList())
                                .build()
                        val responseBuffers = response.toBuffersArray()
                        attachment.responseHeaderBuffer = responseBuffers[0]
                        attachment.responseBodyBuffer = responseBuffers[1]
                        channel.write(responseBuffers[0], attachment, sendResponseHandler)
                    })

            readHeader()
        }

        override fun failed(exc: Throwable?, attachment: AsynchronousClientAttachment?) {
            TODO("Not yet implemented")
        }
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

    private val sendResponseHandler = object : CompletionHandler<Int, AsynchronousClientAttachment> {
        override fun completed(result: Int?, attachment: AsynchronousClientAttachment?) {
            val responseBuffers = attachment?.responseBuffers!!
            when {
                responseBuffers[0].hasRemaining() -> channel.write(responseBuffers[0], attachment, this)
                responseBuffers[1].hasRemaining() -> channel.write(responseBuffers[1], attachment, this)
                else -> {
                    attachment.finish()
                    requestResponseListMs.add(attachment.requestResponseTimeMs!!)
                    taskTimeListMs.add(attachment.taskTimeMs!!)
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: AsynchronousClientAttachment?) {
            TODO("Not yet implemented")
        }
    }.terminateOn { r, a -> channelClosedPredicate(r, a)}

    fun start() = readHeader()

    private fun readHeader() {
        val headerBuffer = ByteBuffer.allocate(Constants.HEADER_SIZE)
        val attachment = AsynchronousClientAttachment(startTimeMs = System.currentTimeMillis(), headerBuffer = headerBuffer)
        channel.read(headerBuffer, attachment, headerHandler)
    }

    override fun close() {
        onClose()
    }
}
