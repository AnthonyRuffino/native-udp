package com.kvara.io.udp;

import com.kvara.io.ParsedMessage;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.worldy.sockiopath.udp.UdpServer;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@ApplicationScoped
public class UdpServerVertical extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(UdpServerVertical.class);

    private final String id;

    @Value("${com.kvara.io.udp.server.port}")
    int port;

    public static Map<String, Integer> BOUND_PORTS = new HashMap<>();

    @Autowired
    @Qualifier("messageParser")
    Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    public UdpServerVertical() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("starting UdpServerVertical");

        UdpServer udpServer = new UdpServer(
                new UdpMessageHandler(vertx, messageParser),
                Executors.newFixedThreadPool(1),
                port
        );

        try {
            int port = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();
            BOUND_PORTS.put(id, port);
            startPromise.complete();
        } catch (InterruptedException | ExecutionException e) {
            startPromise.fail(e.getCause());
        }
    }
}