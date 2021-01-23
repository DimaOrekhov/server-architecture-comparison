package ru.itmo.java.architectures.server.synchronous.nonblocking

import ru.itmo.java.architectures.common.Utils.mean
import ru.itmo.java.architectures.server.domain.ClientWorker
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

class NonBlockingClientWorker(val channel: SocketChannel, selectorOut: Selector, taskPool: ExecutorService) : ClientWorker {

    val channelState = AtomicReference(ChannelState.DEREGISTERED)
    val responseQueue = ConcurrentLinkedQueue<Response>()
    val inputProcessor = NonBlockingClientInputProcessor(this, taskPool, responseQueue, selectorOut)

    private val startTimes = ConcurrentHashMap<Int, Long>()

    private val taskTimeListMs = ConcurrentLinkedDeque<Long>()
    private val requestResponseTimeListMs = ConcurrentLinkedDeque<Long>()

    override val meanRequestResponseTimeMs: Double
        get() = taskTimeListMs.mean()
    override val meanTaskTimeMs: Double
        get() = requestResponseTimeListMs.mean()

    fun saveReceiveTime(id: Int, time: Long) {
        startTimes[id] = time
    }

    fun markProcessed(id: Int) = startTimes[id]?.let { startTime ->
        requestResponseTimeListMs.add(System.currentTimeMillis() - startTime)
    }

    fun addTaskTime(taskTime: Long) {
        taskTimeListMs.add(taskTime)
    }

    override fun shutdown() {}
}
