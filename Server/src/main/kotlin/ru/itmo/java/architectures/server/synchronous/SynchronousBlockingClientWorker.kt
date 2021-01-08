package ru.itmo.java.architectures.server.synchronous

import ru.itmo.java.architectures.common.Utils.readWithSizeFrom
import ru.itmo.java.architectures.common.Utils.writeWithSizeTo
import ru.itmo.java.architectures.protocol.IntArrayMessage
import ru.itmo.java.architectures.server.domain.ClientWorker
import ru.itmo.java.architectures.server.domain.SortCallable
import ru.itmo.java.architectures.server.tasks.Utils.thenApply
import ru.itmo.java.architectures.server.tasks.asTimed
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SynchronousBlockingClientWorker(private val socket: Socket, private val globalThreadPool: ExecutorService) : ClientWorker {

    private val inputExecutor = Executors.newSingleThreadExecutor()
    private val inputStream = DataInputStream(socket.getInputStream())
    private val resultQueue = ConcurrentLinkedQueue<ResultWithTimeMeasurements<Array<Int>>>()
    private val outputExecutor = Executors.newSingleThreadExecutor()
    private val outputStream = DataOutputStream(socket.getOutputStream())

    private val computationTimeListMs = mutableListOf<Long>()
    private val processingTimeListMs = mutableListOf<Long>()

    data class ResultWithTimeMeasurements<T>(
        val result: T,
        val receiveTimeMs: Long,
        val executionTimeMs: Long,
        var totalProcessingTimeMs: Long? = null
    ) {
        fun finish() {
            if (totalProcessingTimeMs != null) {
                totalProcessingTimeMs = System.currentTimeMillis() - receiveTimeMs
            }
        }
    }

    fun start() {
        inputExecutor.submit {
            while (!Thread.interrupted()) {
                val request = readWithSizeFrom(inputStream)
                val receiveTimeMs = System.currentTimeMillis()

                val array = request.elementsList.toTypedArray()
                globalThreadPool.submit(SortCallable(array).asTimed()
                    .thenApply { (result, executionTimeMs) ->
                        resultQueue.add(ResultWithTimeMeasurements(result, receiveTimeMs, executionTimeMs))
                    })
            }
        }

        outputExecutor.submit {
            while (!Thread.interrupted()) {
                if (resultQueue.isEmpty()) {
                    continue
                }
                val resultWithTimeMeasurements = resultQueue.poll()
                val response = IntArrayMessage.newBuilder()
                    .addAllElements(resultWithTimeMeasurements.result.asList())
                    .build()
                response.writeWithSizeTo(outputStream)

                resultWithTimeMeasurements.finish()
                computationTimeListMs.add(resultWithTimeMeasurements.executionTimeMs)
                processingTimeListMs.add(resultWithTimeMeasurements.totalProcessingTimeMs!!)
            }
        }
    }

    override fun close() {
        inputExecutor.shutdown()
        outputExecutor.shutdown()
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}
