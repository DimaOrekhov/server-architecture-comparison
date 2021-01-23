package ru.itmo.java.architectures.server.domain

import java.io.Closeable

interface ClientWorker: Closeable {
    val meanRequestResponseTimeMs: Double
    val meanTaskTimeMs: Double
}
