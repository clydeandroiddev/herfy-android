package com.herfy.utils.poller

import com.skylinedynamics.solosdk.api.calls.OrdersApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject


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
    /*override fun poll(delayValue: Long): Flow<suspend () -> Unit> {
       return channelFlow {
           while(!isClosedForSend){
               delay(delayValue)
               val either = fetchData
               send(either)
           }
       }.flowOn(dispatcher)
    }

    override fun close() {
        dispatcher.cancel()
    }*/
}