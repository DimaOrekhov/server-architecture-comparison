package ru.itmo.java.architectures.server.domain

interface TimedServer: Server {
    val meanRequestResponseTimeMs: Long

    val meanTaskTimeMs: Long

    fun reset()
}
