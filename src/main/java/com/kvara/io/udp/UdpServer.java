package com.kvara.io.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class UdpServer {
    private final ChannelHandler channelHandler;
    private final ExecutorService executor;
    final int port;

    private Optional<Consumer<Integer>> maybePortConsumer = Optional.empty();
    private Optional<Consumer<Exception>> maybeExceptionConsumer = Optional.empty();


    public UdpServer(
            ChannelHandler channelHandler,
            ExecutorService executor,
            int port
    ) {
        this.channelHandler = channelHandler;
        this.executor = executor;
        this.port = port;
    }

    public UdpServer withPortConsumer(Consumer<Integer> portConsumer) {
        this.maybePortConsumer = Optional.of(portConsumer);
        return this;
    }

    public UdpServer withExceptionConsumer(Consumer<Exception> exceptionConsumer) {
        this.maybeExceptionConsumer = Optional.of(exceptionConsumer);
        return this;
    }

    public void startUdpServer() {
        executor.submit(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(channelHandler);

                Channel channel = bootstrap.bind(port).sync().channel();
                int boundPort = getPort(channel);
                maybePortConsumer.ifPresent(portCallback -> portCallback.accept(boundPort));
                channel.closeFuture().await().addListener((closeResult) ->
                        System.out.println("closing UDP server...")
                );
            } catch (Exception ex) {
                maybeExceptionConsumer.ifPresent(exceptionConsumer -> exceptionConsumer.accept(ex));
            } finally {
                try {
                    group.shutdownGracefully();
                } catch (Exception ex) {
                    System.out.println("Error while shutting down the UDP server: " + ex.getMessage());
                }
            }
        });
    }

    private int getPort(Channel channel) {
        SocketAddress socketAddress = channel.localAddress();
        return ((InetSocketAddress) socketAddress).getPort();
    }

}