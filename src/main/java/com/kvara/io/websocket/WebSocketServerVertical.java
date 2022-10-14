package com.kvara.io.websocket;

import com.kvara.io.ParsedMessage;
import com.kvara.io.SockiopathServerVertical;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.session.MapBackedSessionStore;
import io.worldy.sockiopath.websocket.session.SessionStore;
import io.worldy.sockiopath.websocket.session.WebSocketSession;
import io.worldy.sockiopath.websocket.session.WebSocketSessionHandler;
import io.worldy.sockiopath.websocket.ui.WebSocketIndexPageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@ApplicationScoped
public class WebSocketServerVertical extends SockiopathServerVertical {

    @Value("${com.kvara.io.websocket.server.port}")
    int port;

    @Value("${com.kvara.io.websocket.server.htmlTemplatePath}")
    String htmlTemplatePath;

    @Autowired
    @Qualifier("messageParser")
    Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    public WebSocketServerVertical() {
        super();
    }


    @Override
    protected SockiopathServer sockiopathServer() {

        LocalMap<String, WebSocketSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        SessionStore<WebSocketSession> sessionStore = new MapBackedSessionStore(sessionMap) {
            @Override
            public WebSocketSession createSession(ChannelHandlerContext ctx) {
                return new SharedWebSocketSession(ctx);
            }
        };

        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of(
                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, htmlTemplatePath),
                () -> new WebSocketSessionHandler(sessionStore)
        );

        ChannelInitializer<SocketChannel> newHandler = SockiopathServer.basicWebSocketChannelHandler(
                messageHandlerSupplier,
                null
        );

        return new WebSocketServer(
                newHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }

    private static class SharedWebSocketSession extends WebSocketSession implements Shareable {
        SharedWebSocketSession(ChannelHandlerContext context) {
            super(context);
        }
    }
}
