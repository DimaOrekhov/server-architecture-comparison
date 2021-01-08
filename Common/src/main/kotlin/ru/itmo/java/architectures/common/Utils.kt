package ru.itmo.java.architectures.common

import ru.itmo.java.architectures.protocol.IntArrayMessage
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

object Utils {

    fun readWithSizeFrom(inputStream: InputStream): IntArrayMessage {
        val messageSize = DataInputStream(inputStream).readInt()
        val bytes = ByteArray(messageSize)
        inputStream.read(bytes)
        return IntArrayMessage.parseFrom(inputStream)
    }

    fun IntArrayMessage.writeWithSizeTo(outputStream: OutputStream) {
        val bytes = toByteArray()
        DataOutputStream(outputStream).writeInt(bytes.size)
        outputStream.write(bytes)
    }
}
