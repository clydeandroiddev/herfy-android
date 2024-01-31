package com.herfy.ably;

import com.herfy.data.constants.Values;
import com.herfy.util.EnvUtil;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.ChannelState;
import io.ably.lib.realtime.ChannelStateListener;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.Message;

public class Connection {

    private static Connection instance = new Connection();

    private Channel channel;

    private Channel.MessageListener channelListener;

    private Connection() {

    }

    public static Connection getInstance() {
        return instance;
    }

    public void init(String conceptId, final AblyCallback callback) {

        try {

            ClientOptions options;
            if (!EnvUtil.isProduction()) {
                options = new ClientOptions(Values.STAGING_ABLY_API_KEY);
            } else {
                options = new ClientOptions(Values.PROD_ABLY_API_KEY);
            }
            AblyRealtime ablyRealtime = new AblyRealtime(options);

            String channelName = "solo-" + conceptId;

            channel = ablyRealtime.channels.get(channelName);

            channel.on(ChannelState.attached, new ChannelStateListener() {

                @Override
                public void onChannelStateChanged(ChannelStateChange stateChange) {
                    callback.onAttach(stateChange);
                }

            });


            channelListener = new Channel.MessageListener() {

                @Override
                public void onMessage(Message message) {
                    if (message.name.equals("Solo\\Order\\Status")) {
                        callback.onOrderStatusMessageReceived(message);
                    } else {
                        callback.onDriverCoordinatesMessageReceived(message);
                    }
                }
            };

            channel.subscribe(channelListener);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError(e);
        }
    }

    public void unsubscribe() {
        if (channel != null && channelListener != null) {
            channel.unsubscribe(channelListener);
        }
    }
}
