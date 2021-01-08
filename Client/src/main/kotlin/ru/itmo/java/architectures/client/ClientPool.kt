package ru.itmo.java.architectures.client

import java.io.Closeable
import java.util.concurrent.Executors

class ClientPool(
    address: String,
    port: Int,
    nClients: Int,
    private val nRequests: Int,
    private val nElements: Int,
    private val requestDelay: Long
) : Runnable, Closeable {

    private val clientsThreadPool = Executors.newFixedThreadPool(nClients)

    private val clients = Array(nClients) { Client(address, port, nRequests, nElements, requestDelay) }

    override fun run() = clients.forEach { clientsThreadPool.submit(it) }

    override fun close() {
        // TODO: add time report finalization
        clients.forEach { it.close() }
        clientsThreadPool.shutdown()
    }
}
