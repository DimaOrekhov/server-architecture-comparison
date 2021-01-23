package ru.itmo.java.architectures.server.synchronous.nonblocking

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel


open class Response(val id: Int, private val buffers: Array<ByteBuffer>) {
    private var offset = 0
    private var length = buffers.size

    fun writeTo(channel: SocketChannel): Long {
        val nWritten = channel.write(buffers, offset, length)
        for (i in offset..buffers.size) {
            if (buffers[i].hasRemaining()) {
                break
            }
            offset++
            length--
        }
        return nWritten
    }

    fun isEmpty() = offset >= buffers.size
}
