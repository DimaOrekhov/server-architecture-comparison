package ru.itmo.java.architectures.server.domain

interface ClientWorker {
    val meanRequestResponseTimeMs: Double
    val meanTaskTimeMs: Double

    fun shutdown()
}
