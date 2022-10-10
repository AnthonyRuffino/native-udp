package com.kvara.io.websocket;

import com.kvara.io.ParsedMessage;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerVertical.class);

    private final String id;

    @Value("${com.kvara.io.websocket.server.port}")
    int port;

    @Value("${com.kvara.io.websocket.server.htmlTemplatePath}")
    String htmlTemplatePath;

    public static Map<String, Integer> BOUND_PORTS = new HashMap<>();

    @Autowired
    @Qualifier("messageParser")
    Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    public WebSocketServerVertical() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("starting WebSocketsServerVertical");

        WebSocketServer websocketServer = new WebSocketServer(
                new WebSocketServerInitializer(null, htmlTemplatePath, vertx),
                Executors.newFixedThreadPool(1),
                port
        );

        websocketServer
                .withPortConsumer(port -> {
                    logger.info("bound WebSocket port -> %s".formatted(port));
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