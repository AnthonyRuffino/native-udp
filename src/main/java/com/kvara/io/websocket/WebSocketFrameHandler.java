package com.kvara.io.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.vertx.mutiny.core.Vertx;
import io.worldy.sockiopath.websocket.session.SessionStore;
import io.worldy.sockiopath.websocket.session.WebSocketSession;
import io.worldy.sockiopath.websocket.session.WebSocketSessionHandler;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class WebSocketFrameHandler extends WebSocketSessionHandler<WebSocketSession> {

    public WebSocketFrameHandler(Vertx vertx) {
        super(getSessionStore(vertx));

    }

    private static SessionStore<WebSocketSession> getSessionStore(Vertx vertx) {
        LocalMap<String, WebSocketSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        return new SessionStore<>() {
            @Override
            public Function<String, WebSocketSession> get() {
                return sessionMap::get;
            }

            @Override
            public BiFunction<String, WebSocketSession, WebSocketSession> put() {
                return sessionMap::put;
            }

            @Override
            public Function<String, WebSocketSession> remove() {
                return sessionMap::remove;
            }

            @Override
            public Supplier<Integer> size() {
                return sessionMap::size;
            }

            @Override
            public Supplier<Set<String>> keySet() {
                return sessionMap::keySet;
            }

            @Override
            public WebSocketSession createSession(ChannelHandlerContext ctx) {
                return new SharedWebSocketSession(ctx);
            }
        };
    }

    private static class SharedWebSocketSession extends WebSocketSession implements Shareable {
        SharedWebSocketSession(ChannelHandlerContext context) {
            super(context);
        }
    }
}
