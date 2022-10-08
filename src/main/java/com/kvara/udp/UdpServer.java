package com.kvara.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ApplicationScoped
public class UdpServer extends AbstractVerticle {

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

    public UdpServer() {
        this.executor = Executors.newFixedThreadPool(1);
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(io.vertx.core.Promise<Void> startPromise) {
        System.out.println("Starting UdpVerticle");
        startUdpServer(Optional.of(startPromise));
    }

    private void startUdpServer(Optional<Promise<Void>> startPromise) {
        executor.submit(() -> {
            boolean errorAfterStartup = false;
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new UdpMessageHandler(vertx, udpMessageParser, flush));

                System.out.println("binding UDP port: " + port);
                Channel channel = bootstrap.bind(port).sync().channel();
                int boundPort = getPort(channel);
                BOUND_PORTS.put(this.id, boundPort);
                startPromise.ifPresent(Promise::complete);
                System.out.println("bound UDP port: " + boundPort);
                channel.closeFuture().await().addListener((closeResult) ->
                        System.out.println("closing UDP server...")
                );
            } catch (Exception ex) {
                errorAfterStartup = true;
                System.out.println("Error while running the UDP server: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    group.shutdownGracefully();
                } catch (Exception ex) {
                    System.out.println("Error while shutting down the UDP server: " + ex.getMessage());
                } finally {
                    if (errorAfterStartup) {
                        startUdpServer(Optional.empty());
                    } else {
                        System.exit(-1);
                    }
                }
            }
        });
    }

    private int getPort(Channel channel) {
        SocketAddress socketAddress = channel.localAddress();
        return ((InetSocketAddress) socketAddress).getPort();
    }

}