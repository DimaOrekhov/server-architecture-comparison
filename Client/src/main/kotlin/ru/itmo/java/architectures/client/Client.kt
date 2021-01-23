package ru.itmo.java.architectures.client

import ru.itmo.java.architectures.common.Utils.readWithSizeFrom
import ru.itmo.java.architectures.common.Utils.writeWithSizeTo
import ru.itmo.java.architectures.protocol.IntArrayMessage
import java.net.Socket
import kotlin.random.Random

class Client(
    private val address: String,
    private val port: Int,
    private val nRequests: Int,
    private val nElements: Int,
    private val requestDelay: Long,
    private val onFinished: () -> Unit
) : Runnable {

    enum class ClientState { NEW, RUNNING, DONE, FAILED }

    @Volatile
    var state = ClientState.NEW
        private set
    var runningTime: Long = 0
        private set
    var meanRequestResponseTime: Double = 0.0
        private set
    @Volatile
    private lateinit var clientThread: Thread

    var excpetion: Throwable? = null
        private set

    override fun run() {
        val startTime = System.currentTimeMillis()
        state = ClientState.RUNNING
        try {
            val socket = Socket(address, port)
            clientThread = Thread.currentThread()
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            for (i in 1..nRequests) {
                val elements = IntArray(nElements) { Random.nextInt() }
                val request = IntArrayMessage.newBuilder().addAllElements(elements.toList()).build()

                request.writeWithSizeTo(outputStream)
                outputStream.flush()

                val response = readWithSizeFrom(inputStream)

                Thread.sleep(requestDelay)
            }
        } catch (e: Throwable) {
            excpetion = e
            throw e
        } finally {
            runningTime = System.currentTimeMillis() - startTime
            meanRequestResponseTime = runningTime / nRequests.toDouble()
            state = if (excpetion == null) ClientState.DONE else ClientState.FAILED

            onFinished()
        }
    }
}
