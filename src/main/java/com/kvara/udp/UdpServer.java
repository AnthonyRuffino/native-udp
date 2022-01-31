package com.kvara.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@ApplicationScoped
public class UdpServer extends AbstractVerticle {

    private final ExecutorService executor;

    @Value("${com.kvara.udp.UdpServer.port}")
    int port;

    @Value("${com.kvara.udp.UdpServer.flush}")
    boolean flush;

    @Autowired
    @Qualifier("udpMessageParser")
    private Function<ByteBuffer, Optional<UdpParsedMessage>> udpMessageParser;

    public UdpServer() {
        this.executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public void start(io.vertx.core.Promise<Void> startPromise) {
        System.out.println("Starting UdpVerticle");
        startUdpServer();
        startPromise.complete();
    }

    private void startUdpServer() {
        executor.submit(() -> {
            boolean isError = false;
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new UdpMessageHandler(vertx.eventBus(), udpMessageParser, flush));

                System.out.println("binding UDP port: " + port);
                b.bind(port).sync().channel().closeFuture().await();
            } catch (Exception ex) {
                isError = true;
                System.out.println("Error while running the UDP server: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    group.shutdownGracefully();
                } catch (Exception ex) {
                    System.out.println("Error while shutting down the UDP server: " + ex.getMessage());
                } finally {
                    if (isError) {
                        startUdpServer();
                    }

                }
            }
        });
    }


}