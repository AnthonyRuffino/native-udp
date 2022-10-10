package com.kvara.io.websocket.session;

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.shareddata.Shareable;

public class WebSocketSession implements Shareable {
    private final ChannelHandlerContext context;

    public WebSocketSession(ChannelHandlerContext context) {
        this.context = context;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }
}
