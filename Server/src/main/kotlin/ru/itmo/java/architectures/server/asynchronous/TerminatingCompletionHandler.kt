package ru.itmo.java.architectures.server.asynchronous

import java.nio.channels.CompletionHandler

class TerminateCompletionHandlerDecorator<V, A>(
        private val handler: CompletionHandler<V, A>,
        private val terminationPredicate: (V, A) -> Boolean,
        private val onTermination: (V, A) -> Unit = { _, _ -> }
) : CompletionHandler<V, A> {
    override fun completed(result: V, attachment: A) {
        if (terminationPredicate(result, attachment)) {
            onTermination(result, attachment)
            return
        }
        handler.completed(result, attachment)
    }

    override fun failed(exc: Throwable?, attachment: A) {
        handler.failed(exc, attachment)
    }
}

fun <V, A> CompletionHandler<V, A>.terminateOn(onTermination: (V, A) -> Unit = { _, _ -> },
                                               terminationPredicate: (V, A) -> Boolean) =
        TerminateCompletionHandlerDecorator(this, terminationPredicate, onTermination)
