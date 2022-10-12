package com.kvara.io.udp;

import com.kvara.io.ParsedMessage;
import com.kvara.io.SockiopathServerVertical;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.udp.UdpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ApplicationScoped
public class UdpServerVertical extends SockiopathServerVertical {
    @Value("${com.kvara.io.udp.server.port}")
    int port;

    @Autowired
    @Qualifier("messageParser")
    Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    public UdpServerVertical() {
        super();
    }

    @Override
    protected SockiopathServer sockiopathServer() {
        return new UdpServer(
                new UdpMessageHandler(vertx, messageParser),
                Executors.newFixedThreadPool(1),
                port
        );
    }
}
