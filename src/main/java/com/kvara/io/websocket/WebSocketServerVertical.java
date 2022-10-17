package com.kvara.io.websocket;

import com.kvara.io.SharedSockiopathSession;
import com.kvara.io.SockiopathServerVertical;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.vertx.core.shareddata.LocalMap;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.WebSocketServerHandler;
import io.worldy.sockiopath.websocket.ui.WebSocketIndexPageHandler;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@ApplicationScoped
public class WebSocketServerVertical extends SockiopathServerVertical {

    @Value("${com.kvara.io.websocket.server.port}")
    int port;

    @Value("${com.kvara.io.websocket.server.htmlTemplatePath}")
    String htmlTemplatePath;

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    public WebSocketServerVertical() {
        super();
    }


    @Override
    protected SockiopathServer sockiopathServer() {
        LocalMap<String, SockiopathSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        SessionStore<SockiopathSession> sessionStore = SharedSockiopathSession.localMapMapBackedSessionStore(sessionMap);
        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of(
                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, htmlTemplatePath),
                () -> new WebSocketServerHandler(sessionStore, getMessageHandlers(), deliminator)
        );

        ChannelInitializer<SocketChannel> channelInitializer = SockiopathServer.basicWebSocketChannelHandler(
                messageHandlerSupplier,
                null
        );

        return new WebSocketServer(
                channelInitializer,
                Executors.newFixedThreadPool(1),
                port
        );
    }
}
