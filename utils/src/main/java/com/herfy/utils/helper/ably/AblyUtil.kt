package com.herfy.utils.helper.ably

import com.herfy.utils.constants.Values
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Message

class AblyUtil {

    private var ablyRealtime: AblyRealtime? = null

    init {
        ablyRealtime = AblyRealtime(getOptions())
    }

    private fun getOptions(): ClientOptions {
        return if (!EnvUtil.isProduction()) {
            ClientOptions(Values.STAGING_ABLY_API_KEY)
        } else {
            ClientOptions(Values.PROD_ABLY_API_KEY)
        }
    }

    private fun getOrderStatusUpdateChannelName(orderId: String): String {
        return String.format(
            "solo:merchant-%s:order-%s:update",
            CacheUtil.getInstance().concept.id,
            orderId
        )
    }

    private fun getOrderDriverAssignedChannelName(orderId: String): String {
        return String.format(
            "solo:merchant-%s:order-%s:driver:assign",
            CacheUtil.getInstance().concept.id,
            orderId
        )
    }

    private fun getOrderDriverLocationUpdateChannelName(orderId: String): String {
        return String.format(
            "solo:merchant-%s:order-%s:driver:location:update",
            CacheUtil.getInstance().concept.id,
            orderId
        )
    }

    private fun subscribe(channelName: String, onMessageReceived: (Message) -> Unit) {
        ablyRealtime?.apply {
            channels?.get(channelName)?.let { channel ->
                val channelListener = Channel.MessageListener {
                    onMessageReceived(it)
                }
                channel.subscribe(channelListener)
            }
        }
    }

    fun subscribeToOrderStatusUpdate(
        orderId: String,
        onMessageReceived: (Message) -> Unit
    ) {
        subscribe(
            getOrderStatusUpdateChannelName(orderId),
            onMessageReceived
        )
    }

    fun subscribeToOrderDriverAssign(
        orderId: String,
        onMessageReceived: (Message) -> Unit
    ) {
        subscribe(
            getOrderDriverAssignedChannelName(orderId),
            onMessageReceived
        )
    }

    fun subscribeToOrderDriverLocationUpdate(
        orderId: String,
        onMessageReceived: (Message) -> Unit
    ) {
        subscribe(
            getOrderDriverLocationUpdateChannelName(orderId),
            onMessageReceived
        )
    }

    private fun detach(channelName: String) {
        ablyRealtime?.apply {
            channels?.get(channelName)?.detach()
        }
    }

    fun detachAll() {
        ablyRealtime?.apply {
            channels?.values?.forEach {
                it.detach()
            }
        }
    }

    fun detachToOrderStatusUpdate(orderId: String) {
        detach(getOrderStatusUpdateChannelName(orderId))
    }

    fun detachToOrderDriverAssign(orderId: String) {
        detach(getOrderDriverAssignedChannelName(orderId))
    }

    fun detachToOrderDriverLocationUpdate(orderId: String) {
        detach(getOrderDriverLocationUpdateChannelName(orderId))
    }

    companion object {
        private var _instance = AblyUtil()
        val instance: AblyUtil
            get() = synchronized(this) {
                _instance
            }
    }

}