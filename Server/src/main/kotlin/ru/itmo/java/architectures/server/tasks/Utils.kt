package ru.itmo.java.architectures.server.tasks

import java.util.concurrent.Callable

object Utils {
    fun <V, R> Callable<V>.thenApply(function: (V) -> R)  = Callable { function(call()) }
}
