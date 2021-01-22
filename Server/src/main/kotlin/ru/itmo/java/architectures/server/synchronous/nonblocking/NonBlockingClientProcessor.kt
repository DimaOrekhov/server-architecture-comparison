package ru.itmo.java.architectures.server.synchronous.nonblocking

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService

class NonBlockingClientProcessor(val channel: SocketChannel, taskPool: ExecutorService) {
    @Volatile
    var channelState = ChannelState.NEW
    val responseQueue = ConcurrentLinkedQueue<ByteBuffer>()
    val inputProcessor = NonBlockingClientInputProcessor(taskPool, responseQueue)
}
