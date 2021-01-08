package ru.itmo.java.architectures.server.domain

interface TimedServer: Server {
    fun getJobExecutionTimesMs(): List<Long>

    fun getRequestResponseTimesMs(): List<Long>
}
