package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.Utils.thenApply
import ru.itmo.java.architectures.server.tasks.asTimed
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

class NonBlockingClientWorker(private val taskPool: ExecutorService) {
    private val headBuffer: ByteBuffer = ByteBuffer.allocate(4)
    private lateinit var bodyBuffer: ByteBuffer
    private var isReadingHeader: Boolean = true

    val isAvailableForRead = AtomicBoolean(true)
    val currentBuffer: ByteBuffer?
        get() = when {
            headBuffer.hasRemaining() -> headBuffer
            bodyBuffer.hasRemaining() -> if (isReadingHeader) null else bodyBuffer
            else -> null
        }

    fun maybeSubmitTask() {
        when {
            isReadingHeader -> {
                if (headBuffer.hasRemaining()) {
                    return
                }

                headBuffer.flip()
                val size = headBuffer.int
                bodyBuffer = ByteBuffer.allocate(size)
                isReadingHeader = false
            }
            !isFull() -> return
        }

        isAvailableForRead.set(false)
        try {
            bodyBuffer.flip()
            val request = IntArrayMessage.parseFrom(bodyBuffer)
            taskPool.submit(SortCallable<Int>(request.elementsList.toTypedArray())
                    .thenApply { }
                    .asTimed()
                    .thenApply { })
        } finally {
            isAvailableForRead.set(true)
        }
    }

    private fun isFull(): Boolean = !headBuffer.hasRemaining() && !bodyBuffer.hasRemaining()
}
