package ru.itmo.java.architectures.client

import ru.itmo.java.architectures.common.Utils.readWithSizeFrom
import ru.itmo.java.architectures.common.Utils.writeWithSizeTo
import ru.itmo.java.architectures.protocol.IntArrayMessage
import java.io.Closeable
import java.net.Socket
import kotlin.random.Random

class Client(
    private val address: String,
    private val port: Int,
    private val nRequests: Int,
    private val nElements: Int,
    private val requestDelay: Long
) : Runnable, Closeable {

    private val processingTimeListMs = mutableListOf<Long>()
    private lateinit var clientThread: Thread

    override fun run() =
        Socket(address, port).use { socket ->
            clientThread = Thread.currentThread()
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            for (i in 1..nRequests) {
                val elements = IntArray(nElements) { Random.nextInt() }
                val request = IntArrayMessage.newBuilder().addAllElements(elements.toList()).build()

                val startTime = System.currentTimeMillis()
                request.writeWithSizeTo(outputStream)
                outputStream.flush()

                readWithSizeFrom(inputStream)
                processingTimeListMs.add(System.currentTimeMillis() - startTime)

                Thread.sleep(requestDelay)
            }
        }

    override fun close() = clientThread.interrupt()
}
