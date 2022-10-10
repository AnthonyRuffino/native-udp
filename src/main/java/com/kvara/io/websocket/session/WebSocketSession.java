package com.kvara.io.websocket.session;

import io.netty.channel.ChannelHandlerContext;

public class WebSocketSession {
    private final ChannelHandlerContext context;
    private final String token;

    public WebSocketSession(ChannelHandlerContext context, String token) {
        this.context = context;
        this.token = token;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }

    public String getToken() {
        return token;
    }
}
