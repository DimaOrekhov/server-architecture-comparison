package ru.itmo.java.architectures.server.asynchronous

import ru.itmo.java.architectures.common.Constants
import java.nio.ByteBuffer

class AsynchronousClientAttachment(
        var startTimeMs: Long? = null,
        var taskTimeMs: Long? = null,
        var requestResponseTimeMs: Long? = null,
        var headerBuffer: ByteBuffer = ByteBuffer.allocate(Constants.HEADER_SIZE),
        var bodyBuffer: ByteBuffer? = null,
        var responseHeaderBuffer: ByteBuffer? = null,
        var responseBodyBuffer: ByteBuffer? = null
) {
    val responseBuffers: Array<ByteBuffer>?
        get() = responseHeaderBuffer?.let { header ->
            responseBodyBuffer?.let { body ->
                arrayOf(header, body)
            }
        }

    fun finish() {
        if (requestResponseTimeMs == null) {
            requestResponseTimeMs = System.currentTimeMillis() - startTimeMs!!
        }
    }
}
