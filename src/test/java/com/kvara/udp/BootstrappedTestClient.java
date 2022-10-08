package com.kvara.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BootstrappedTestClient<T> {
    protected final String host;
    protected final int port;
    protected final int connectTimeoutMilliseconds;
    protected Channel channel;
    protected EventLoopGroup workGroup = new NioEventLoopGroup();
    protected T response;

    protected boolean shutdownOnComplete = true;

    abstract protected T parse(ByteBuffer byteBuffer) throws Exception;


    /**
     * Constructor
     *
     * @param port {@link Integer} port of server
     */
    public BootstrappedTestClient(String host, int port, int connectTimeoutMilliseconds) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
    }

    public T getResponse() {
        return response;
    }

    public void runTest(byte[] messageBytes, T expectedResponse) {
        runTest(
                messageBytes,
                () -> assertEquals(expectedResponse, getResponse())
        );
    }

    public void runTest(byte[] messageBytes, Runnable assertion) {
        runTest(
                messageBytes,
                assertion,
                50,
                50
        );
    }

    public void runTest(byte[] messageBytes, Runnable assertion, int writeTimeout, int closeTimeout) {
        try {
            Channel channel = startup();

            boolean writeWasSuccessful = channel
                    .writeAndFlush(Unpooled.wrappedBuffer(messageBytes))
                    .await(writeTimeout, TimeUnit.MILLISECONDS);

            if (!writeWasSuccessful) {
                throw new RuntimeException("Client took too long to write to channel!");
            }

            channel.closeFuture().await(closeTimeout, TimeUnit.MILLISECONDS);
            assertion.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (shutdownOnComplete) {
                shutdown();
            }
        }
    }

    /**
     * Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    public Channel startup() throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(workGroup);
        b.channel(NioDatagramChannel.class);
        b.handler(new ChannelInitializer<DatagramChannel>() {
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                datagramChannel.pipeline().addLast(
                        new SimpleChannelInboundHandler<DatagramPacket>() {

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, io.netty.channel.socket.DatagramPacket msg) throws Exception {
                                response = parse(msg.content().nioBuffer());
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                            }
                        }
                );
            }
        });
        ChannelFuture channelFuture = b.connect("localhost", this.port);
        if (!channelFuture.await(connectTimeoutMilliseconds, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client took too long to connect");
        }
        this.channel = channelFuture.channel();
        return this.channel;
    }

    /**
     * Shutdown a client
     */
    public void shutdown() {
        workGroup.shutdownGracefully();
    }
}
