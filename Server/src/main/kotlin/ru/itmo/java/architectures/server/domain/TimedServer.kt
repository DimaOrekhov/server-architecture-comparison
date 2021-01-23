package ru.itmo.java.architectures.server.domain

interface TimedServer: Server {
    val meanRequestResponseTimeMs: Double

    val meanTaskTimeMs: Double

    fun reset()
}
