package ru.itmo.java.architectures.experiment

enum class ServerArchitectureType(val showName: String) {
    SYNCHRONOUS_BLOCKING("Synchronous blocking"),
    NONBLOCKING("Nonblocking"),
    ASYNCHRONOUS("Asynchronous")
}
