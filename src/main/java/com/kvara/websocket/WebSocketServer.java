package com.kvara.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class WebSocketServer {
    private final ChannelHandler channelHandler;
    private final ExecutorService executor;
    final int port;

    private Optional<Consumer<Integer>> maybePortConsumer = Optional.empty();
    private Optional<Consumer<Exception>> maybeExceptionConsumer = Optional.empty();


    public WebSocketServer(
            ChannelHandler channelHandler,
            ExecutorService executor,
            int port
    ) {
        this.channelHandler = channelHandler;
        this.executor = executor;
        this.port = port;
    }

    public WebSocketServer withPortConsumer(Consumer<Integer> portConsumer) {
        this.maybePortConsumer = Optional.of(portConsumer);
        return this;
    }

    public WebSocketServer withExceptionConsumer(Consumer<Exception> exceptionConsumer) {
        this.maybeExceptionConsumer = Optional.of(exceptionConsumer);
        return this;
    }

    public void startServer() {
        executor.submit(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(channelHandler);

                Channel ch = b.bind(port).sync().channel();

                System.out.println("Open your web browser and navigate to " +
                        (false ? "https" : "http") + "://127.0.0.1:" + port + '/');

                maybePortConsumer.ifPresent(p -> p.accept(port));

                ch.closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }

    private int getPort(Channel channel) {
        SocketAddress socketAddress = channel.localAddress();
        return ((InetSocketAddress) socketAddress).getPort();
    }

}