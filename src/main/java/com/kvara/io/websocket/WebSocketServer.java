package com.kvara.io.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

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

                Channel channel = b.bind(port).sync().channel();

                int boundPort = getPort(channel);
                maybePortConsumer.ifPresent(p -> p.accept(boundPort));

                channel.closeFuture().await().addListener((closeResult) ->
                        logger.info("closing WebSocketServer")
                );
            } catch (InterruptedException e) {
                maybeExceptionConsumer.ifPresent(exceptionConsumer -> exceptionConsumer.accept(e));
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