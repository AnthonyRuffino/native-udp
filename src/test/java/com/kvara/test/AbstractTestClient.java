package com.kvara.test;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTestClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTestClient.class);

    protected final String host;
    protected final int port;
    protected final int connectTimeout;
    public CountDownLatch latch;
    protected Channel channel;
    protected EventLoopGroup workGroup = new NioEventLoopGroup();

    protected final Map<Long, StatefulAssertion> assertions = new ConcurrentHashMap<>();

    public abstract static class StatefulAssertion {
        abstract void runAssertion() throws Exception;

        protected Object response;

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }
    }

    public interface Assertion {
        void runAssertion(Object response) throws Exception;
    }

    /**
     * Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    public abstract void startup();

    /**
     * Constructor
     *
     * @param port {@link Integer} port of server
     */
    public AbstractTestClient(String host, int port, int connectTimeout) {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
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
        }
    }

    public void sendMessage(byte[] messageBytes, int writeTimeout) throws InterruptedException {
        sendMessage(Unpooled.wrappedBuffer(messageBytes), writeTimeout);
    }

    public void sendMessage(Object message, int writeTimeout) throws InterruptedException {
        boolean writeWasSuccessful = channel
                .writeAndFlush(message)
                .await(writeTimeout, TimeUnit.MILLISECONDS);

        if (!writeWasSuccessful) {
            throw new RuntimeException("Client took too long to write to channel!");
        }
    }

    protected void debugMessage(String message) {
        try {
            logger.debug("Latch   : " + latch.getCount());
            logger.debug("Message : " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdown a client
     *
     * @return
     */
    public Future<?> shutdown() {
        return workGroup.shutdownGracefully();
    }
}
