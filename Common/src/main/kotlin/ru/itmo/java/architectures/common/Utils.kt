package ru.itmo.java.architectures.common

import ru.itmo.java.architectures.protocol.IntArrayMessage
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object Utils {

    fun readWithSizeFrom(inputStream: InputStream): IntArrayMessage {
        val messageSize = DataInputStream(inputStream).readInt()
        val bytes = ByteArray(messageSize)
        inputStream.read(bytes)
        return IntArrayMessage.parseFrom(bytes)
    }

    fun IntArrayMessage.writeWithSizeTo(outputStream: OutputStream) {
        val bytes = toByteArray()
        DataOutputStream(outputStream).writeInt(bytes.size)
        outputStream.write(bytes)
    }

    fun IntArrayMessage.toBuffersArray(): Array<ByteBuffer> {
        val bytes = toByteArray()
        val size = bytes.size
        val headerBuffer = ByteBuffer.allocate(4)
        headerBuffer.putInt(size)
        headerBuffer.flip()
        val bodyBuffer = ByteBuffer.wrap(bytes)
        return arrayOf(headerBuffer, bodyBuffer)
    }

    fun Array<Int>.toIntArrayMessage(): IntArrayMessage =
            IntArrayMessage.newBuilder().addAllElements(this.toList()).build()

    fun IntArray.toIntArrayMessage(): IntArrayMessage =
            IntArrayMessage.newBuilder().addAllElements(this.toList()).build()

    @kotlin.jvm.JvmName("meanOfLong")
    fun Collection<Long>.mean() = sum() / size.toDouble()

    @kotlin.jvm.JvmName("meanOfInt")
    fun Collection<Int>.mean() = sum() / size.toDouble()

    @kotlin.jvm.JvmName("meanOfDouble")
    fun Collection<Double>.mean() = sum() / size.toDouble()

    fun whileNotInterrupted(body: () -> Unit) {
        while (!Thread.interrupted()) {
            body()
        }
    }
}
