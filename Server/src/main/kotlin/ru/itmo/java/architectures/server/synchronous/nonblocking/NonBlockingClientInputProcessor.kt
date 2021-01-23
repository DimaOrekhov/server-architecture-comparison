package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.common.Utils.toBuffersArray
import ru.itmo.java.architectures.common.Utils.toIntArrayMessage
import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.Utils.thenApply
import ru.itmo.java.architectures.server.tasks.asTimed
import java.nio.ByteBuffer
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

class NonBlockingClientInputProcessor(private val client: NonBlockingClientWorker,
                                      private val taskPool: ExecutorService,
                                      private val responseQueue: ConcurrentLinkedQueue<Response>,
                                      private val selectorOut: Selector) {
    private val headBuffer: ByteBuffer = ByteBuffer.allocate(Constants.HEADER_SIZE)
    private lateinit var bodyBuffer: ByteBuffer

    private enum class ReadState {
        READING_HEADER, READING_BODY;
    }
    private var readState = ReadState.READING_HEADER
    private var responseCounter = AtomicInteger(0)

    val currentBuffer: ByteBuffer
        get() = when (readState) {
            ReadState.READING_HEADER -> headBuffer
            ReadState.READING_BODY -> bodyBuffer
        }.apply {
            if (currentMessageReceiveTimeMs == null) {
                currentMessageReceiveTimeMs = System.currentTimeMillis()
            }
        }

    private var currentMessageReceiveTimeMs: Long? = null

    fun maybeSubmitTask() {
        when (readState) {
            ReadState.READING_HEADER -> {
                if (headBuffer.hasRemaining()) {
                    return
                }

                headBuffer.flip()
                val size = headBuffer.int
                bodyBuffer = ByteBuffer.allocate(size)
                headBuffer.clear()

                readState = ReadState.READING_BODY
            }
            ReadState.READING_BODY -> {
                if (bodyBuffer.hasRemaining()) {
                    return
                }

                headBuffer.clear()
                bodyBuffer.flip()
                val request = IntArrayMessage.parseFrom(bodyBuffer)
                val capturedReceiveTimeMs = currentMessageReceiveTimeMs!!
                taskPool.submit(SortCallable<Int>(request.elementsList.toTypedArray())
                        .thenApply { result ->
                            val responseBuffers = result.toIntArrayMessage().toBuffersArray()
                            val responseId = responseCounter.getAndIncrement()
                            val response = Response(responseId, responseBuffers)
                            client.responseQueue.add(response)
                            client.channelState.compareAndSet(ChannelState.DEREGISTERED, ChannelState.NEW)
                            selectorOut.wakeup()
                            return@thenApply responseId
                        }
                        .asTimed()
                        .thenApply { (responseId, taskTime) ->
                            client.addTaskTime(taskTime)
                            client.saveReceiveTime(responseId, capturedReceiveTimeMs)
                        })

                currentMessageReceiveTimeMs = null
                readState = ReadState.READING_HEADER
            }
        }
    }
}
