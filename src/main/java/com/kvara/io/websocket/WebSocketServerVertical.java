package com.kvara.io.websocket;

import com.kvara.io.ParsedMessage;
import com.kvara.io.SockiopathServerVertical;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;

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
        return new WebSocketServer(
                new WebSocketServerInitializer(null, htmlTemplatePath, vertx),
                Executors.newFixedThreadPool(1),
                port
        );
    }
}
