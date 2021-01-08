package ru.itmo.java.architectures.server.domain

import java.io.Closeable

interface Server: Closeable {
    fun start()
}
