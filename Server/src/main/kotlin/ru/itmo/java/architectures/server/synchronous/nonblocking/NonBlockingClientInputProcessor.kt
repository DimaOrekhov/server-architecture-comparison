package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.common.Constants
import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.Utils.thenApply
import ru.itmo.java.architectures.server.tasks.asTimed
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService

class NonBlockingClientInputProcessor(private val taskPool: ExecutorService, private val responseQueue: ConcurrentLinkedQueue<ByteBuffer>) {
    private val headBuffer: ByteBuffer = ByteBuffer.allocate(Constants.HEADER_SIZE)
    private lateinit var bodyBuffer: ByteBuffer

    private enum class ReadState {
        READING_HEADER, READING_BODY;
    }
    private var readState = ReadState.READING_HEADER

    val currentBuffer: ByteBuffer
        get() = when (readState) {
            ReadState.READING_HEADER -> headBuffer
            ReadState.READING_BODY -> bodyBuffer
        }

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
                taskPool.submit(SortCallable<Int>(request.elementsList.toTypedArray())
                        .thenApply { } // Put into queue and wakeup
                        .asTimed()
                        .thenApply { })

                readState = ReadState.READING_HEADER
            }
        }
    }
}
