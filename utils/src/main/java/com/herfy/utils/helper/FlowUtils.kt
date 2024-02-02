package com.herfy.utils.helper

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun <T> Flow<T>.throttleFirst(windowDuration: Duration): Flow<T> {
    var job: Job? = null

    return onCompletion { job?.cancel() }.run {
        flow {
            coroutineScope {
                collect { value ->
                    if (job?.isActive != true) {
                        emit(value)
                        job = launch { delay(windowDuration) }
                    }
                }
            }
        }
    }
}