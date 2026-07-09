package com.herfy.main.session

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

object SessionExpiryManager {

    private val hasPendingSessionExpiry = AtomicBoolean(false)
    private val isDispatching = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var listener: (() -> Unit)? = null

    fun setListener(listener: (() -> Unit)?) {
        this.listener = listener
        dispatchIfNeeded()
    }

    fun notifySessionExpired() {
        hasPendingSessionExpiry.set(true)
        dispatchIfNeeded()
    }

    fun consume() {
        hasPendingSessionExpiry.set(false)
        isDispatching.set(false)
    }

    fun releaseForRetry() {
        isDispatching.set(false)
        dispatchIfNeeded()
    }

    private fun dispatchIfNeeded() {
        val callback = listener ?: return
        if (!hasPendingSessionExpiry.get()) return
        if (!isDispatching.compareAndSet(false, true)) return

        mainHandler.post {
            callback.invoke()
        }
    }
}
