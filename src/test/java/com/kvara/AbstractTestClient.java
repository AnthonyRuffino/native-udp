package com.kvara;

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

public abstract class AbstractTestClient {
    protected final String host;
    protected final int port;
    protected CountDownLatch latch;
    protected Channel channel;
    protected EventLoopGroup workGroup = new NioEventLoopGroup();

    protected final Map<Long, StatefulAssertion> assertions = new ConcurrentHashMap<>();
    protected boolean debug;

    public abstract static class StatefulAssertion {
        abstract void runAssertion() throws Exception;

        protected DatagramPacket response;

        public DatagramPacket getResponse() {
            return response;
        }

        public void setResponse(DatagramPacket response) {
            this.response = response;
        }
    }

    public interface Assertion {
        void runAssertion(DatagramPacket response) throws Exception;
    }

    /**
     * Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    protected abstract Channel startup(int connectTimeout);

    /**
     * Constructor
     *
     * @param port {@link Integer} port of server
     */
    public AbstractTestClient(String host, int port, int connectTimeout) {
        this.host = host;
        this.port = port;
        this.channel = startup(connectTimeout);
    }

    public AbstractTestClient withAssertions(List<Assertion> assertionsList) {
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

    public AbstractTestClient withAssertion(Assertion assertion) {
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

    protected void debugMessage(DatagramPacket msg) {
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
    protected void shutdown() {
        workGroup.shutdownGracefully();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
