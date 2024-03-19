package com.herfy.utils.helper.ably

import com.herfy.utils.constants.Values
import com.herfy.utils.helper.EnvUtil
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ChannelStateListener
import io.ably.lib.types.ClientOptions

class Connection(connection: Connection) {

    private val instance: Connection = connection

    private var channel: Channel? = null

    private var channelListener: Channel.MessageListener? = null

    fun getInstance(): Connection {
        return instance
    }

    fun init(conceptId: String, callback: AblyCallback) {
        try {
            val options: ClientOptions
            options = if (!EnvUtil.isProduction()) {
                ClientOptions(Values.STAGING_ABLY_API_KEY)
            } else {
                ClientOptions(Values.PROD_ABLY_API_KEY)
            }
            val ablyRealtime = AblyRealtime(options)
            val channelName = "solo-$conceptId"
            channel = ablyRealtime.channels[channelName]

            channel?.on(
                ChannelState.attached,
                ChannelStateListener { stateChange -> callback.onAttach(stateChange) })
            channelListener = Channel.MessageListener { message ->
                if (message.name == "Solo\\Order\\Status") {
                    callback.onOrderStatusMessageReceived(message)
                } else {
                    callback.onDriverCoordinatesMessageReceived(message)
                }
            }
            channel?.subscribe(channelListener)
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onError(e)
        }
    }

    fun unsubscribe() {
        if (channel != null && channelListener != null) {
            channel!!.unsubscribe(channelListener)
        }
    }
}