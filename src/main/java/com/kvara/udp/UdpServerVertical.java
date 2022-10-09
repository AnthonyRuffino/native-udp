package com.kvara.udp;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ApplicationScoped
public class UdpServerVertical extends AbstractVerticle {

    private final String id;

    private final ExecutorService executor;

    @Value("${com.kvara.udp.UdpServer.port}")
    int port;

    static Map<String, Integer> BOUND_PORTS = new HashMap<>();

    @Value("${com.kvara.udp.UdpServer.flush}")
    boolean flush;

    @Autowired
    @Qualifier("udpMessageParser")
    private Function<ByteBuffer, Optional<UdpParsedMessage>> udpMessageParser;

    public UdpServerVertical() {
        this.executor = Executors.newFixedThreadPool(1);
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("... Starting UdpServerVertical");

        UdpServer udpServer = new UdpServer(
                new UdpMessageHandler(vertx, udpMessageParser, flush),
                Executors.newFixedThreadPool(1),
                port
        );

        udpServer
                .withPortConsumer(port -> {
                    System.out.println("... bound UDP port ->" + port);
                    BOUND_PORTS.put(this.id, port);
                    startPromise.tryComplete();
                })
                .withExceptionConsumer(ex -> {
                    ex.printStackTrace();
                    udpServer.startUdpServer();
                })
                .startUdpServer();
    }
}