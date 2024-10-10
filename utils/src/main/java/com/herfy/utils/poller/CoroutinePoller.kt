package com.herfy.utils.poller


import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class CoroutinePoller(
    private val dispatcher: CoroutineDispatcher,
    private val fetchData: suspend () -> Unit
) {
    var job: Job? = null

    fun poll(delay: Long) = callbackFlow {
        job = launch(dispatcher) {
            while (isActive) {
                val data = fetchData
                trySend(data)
                delay(delay)
            }
        }
        awaitClose {
            cancel()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

}