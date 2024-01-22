package com.herfy.ably;

import io.ably.lib.realtime.ChannelStateListener;
import io.ably.lib.types.Message;

public interface AblyCallback {

    void onAttach(ChannelStateListener.ChannelStateChange stateChange);

    void onOrderStatusMessageReceived(Message message);

    void onDriverCoordinatesMessageReceived(Message message);

    void onError(Exception e);

}
