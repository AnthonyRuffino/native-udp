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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UdpBootstrappedTestClient {
    private final String host;
    private final int port;
    private CountDownLatch latch;
    protected Channel channel;
    protected EventLoopGroup workGroup = new NioEventLoopGroup();

    private final Map<Long, StatefulAssertion> assertions = new ConcurrentHashMap<>();
    protected boolean debug;

    public abstract static class StatefulAssertion {
        abstract void runAssertion() throws Exception;

        protected DatagramPacket response;
    }

    public interface Assertion {
        void runAssertion(DatagramPacket response) throws Exception;
    }

    /**
     * Constructor
     *
     * @param port {@link Integer} port of server
     */
    public UdpBootstrappedTestClient(String host, int port, int connectTimeout) {
        this.host = host;
        this.port = port;
        this.channel = startup(connectTimeout);
    }

    public UdpBootstrappedTestClient withAssertions(List<Assertion> assertionsList) {
        int numberOfAssertions = assertionsList.size();
        this.assertions.clear();
        this.latch = new CountDownLatch(numberOfAssertions);

        long index = numberOfAssertions;
        for (Assertion assertion : assertionsList) {
            this.assertions.put(index--, new StatefulAssertion() {
                @Override
                void runAssertion() throws Exception {
                    assertion.runAssertion(this.response);
                }
            });
        }
        return this;
    }

    public UdpBootstrappedTestClient withAssertion(Assertion assertion) {
        return withAssertions(List.of(assertion));
    }


    public void checkAssertions(int completionTimeout) {
        try {
            if (!latch.await(completionTimeout, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("The test context did not complete in time!");
            }
            for (var assertion : assertions.entrySet()) {
                assertion.getValue().runAssertion();
            }
            channel.closeFuture().await(completionTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //shutdown();
        }
    }

    public void sendMessage(byte[] messageBytes, int writeTimeout) throws InterruptedException {
        boolean writeWasSuccessful = channel
                .writeAndFlush(Unpooled.wrappedBuffer(messageBytes))
                .await(writeTimeout, TimeUnit.MILLISECONDS);

        if (!writeWasSuccessful) {
            throw new RuntimeException("Client took too long to write to channel!");
        }
    }

    /**
     * Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    private Channel startup(int connectTimeout) {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new ChannelInitializer<DatagramChannel>() {
                protected void initChannel(DatagramChannel datagramChannel) {
                    datagramChannel.pipeline().addLast(
                            new SimpleChannelInboundHandler<DatagramPacket>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                                    debugMessage(msg);
                                    assertions.get(latch.getCount()).response = msg.copy();
                                    latch.countDown();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelActive(ctx);
                                }
                            }
                    );
                }
            });
            ChannelFuture channelFuture = b.connect(host, this.port);
            if (!channelFuture.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Client took too long to connect");
            }
            this.channel = channelFuture.channel();
            return this.channel;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void debugMessage(DatagramPacket msg) {
        if (this.debug) {
            try {
                System.out.println("#################### Latch: " + latch.getCount());
                System.out.println(Charset.defaultCharset().decode(msg.copy().content().nioBuffer()));
                System.out.println("####################");
            } catch (Exception e) {
                System.out.println("fwefeargreageargheargharehtrh");
                e.printStackTrace();
            }
        }
    }

    /**
     * Shutdown a client
     */
    private void shutdown() {
        workGroup.shutdownGracefully();
    }
}
