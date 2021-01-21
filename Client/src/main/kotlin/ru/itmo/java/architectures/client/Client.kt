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

    @Volatile
    var isDone = false
        private set
    var runningTime: Long = 0
        private set
    @Volatile
    private lateinit var clientThread: Thread

    override fun run() {
        val startTime = System.currentTimeMillis()
        Socket(address, port).use { socket ->
            clientThread = Thread.currentThread()
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // Do I need to check interruption?
            for (i in 1..nRequests) {
                val elements = IntArray(nElements) { Random.nextInt() }
                val request = IntArrayMessage.newBuilder().addAllElements(elements.toList()).build()

                request.writeWithSizeTo(outputStream)
                outputStream.flush()

                readWithSizeFrom(inputStream)

                Thread.sleep(requestDelay)
            }
        }
        runningTime = System.currentTimeMillis() - startTime
        isDone = true
    }

    // TODO: do I need this method at all?
    override fun close() = clientThread.interrupt()
}
