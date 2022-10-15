package com.kvara.io;

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;

public class SharedSockiopathSession extends SockiopathSession implements Shareable {
    public SharedSockiopathSession(ChannelHandlerContext context) {
        super(context);
    }

    public static SessionStore<SockiopathSession> localMapMapBackedSessionStore(LocalMap<String, SockiopathSession> sessionMap) {
        return new MapBackedSessionStore(sessionMap) {
            @Override
            public SockiopathSession createSession(ChannelHandlerContext ctx) {
                return new SharedSockiopathSession(ctx);
            }
        };
    }
}
