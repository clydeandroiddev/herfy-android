package com.herfy.utils.helper.ably

import io.ably.lib.realtime.ChannelStateListener

interface AblyCallback {
    fun onAttach(stateChange: ChannelStateListener.ChannelStateChange?)

    fun onOrderStatusMessageReceived(message: io.ably.lib.types.Message?)

    fun onDriverCoordinatesMessageReceived(message: io.ably.lib.types.Message?)

    fun onError(e: Exception?)
}