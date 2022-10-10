package com.kvara.websocket;

import com.kvara.ParsedMessage;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ApplicationScoped
public class WebSocketServerVertical extends AbstractVerticle {

    private final String id;

    @Value("${com.kvara.websockets.server.port}")
    int port;

    static Map<String, Integer> BOUND_PORTS = new HashMap<>();

    @Autowired
    @Qualifier("messageParser")
    private Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    public WebSocketServerVertical() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("... Starting WebSocketsServerVertical");

        WebSocketServer websocketServer = new WebSocketServer(
                new WebSocketServerInitializer(null),
                Executors.newFixedThreadPool(1),
                port
        );

        websocketServer
                .withPortConsumer(port -> {
                    System.out.println("... bound WebSocket port ->" + port);
                    BOUND_PORTS.put(this.id, port);
                    startPromise.tryComplete();
                })
                .withExceptionConsumer(ex -> {
                    ex.printStackTrace();
                    websocketServer.startServer();
                })
                .startServer();
    }
}