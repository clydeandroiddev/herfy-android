package com.herfy.utils.poller

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

interface Poller {
    fun poll(delayValue: Long): Flow<suspend () -> Unit>
    fun close()
}