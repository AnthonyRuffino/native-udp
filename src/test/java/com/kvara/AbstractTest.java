package com.kvara;

import com.kvara.io.udp.UdpServerVertical;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.udp.UdpServer;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    public void sendMessage(Channel channel, byte[] messageBytes, int writeTimeout) throws InterruptedException {
        sendMessage(channel, Unpooled.wrappedBuffer(messageBytes), writeTimeout);
    }

    public void sendMessage(Channel channel, Object message, int writeTimeout) throws InterruptedException {
        boolean writeWasSuccessful = channel
                .writeAndFlush(message)
                .await(writeTimeout, TimeUnit.MILLISECONDS);

        if (!writeWasSuccessful) {
            throw new RuntimeException("Client took too long to write to channel!");
        }
    }

    protected void sendMessage(Channel channel, String message) throws InterruptedException {
        sendMessage(channel, message, 1000);
    }

    protected void sendMessage(Channel channel, String message, int timeoutMillis) throws InterruptedException {
        if (!channel.writeAndFlush(new TextWebSocketFrame(message)).await(timeoutMillis)) {
            fail("sending message took too long: " + message);
        }
    }

    protected void awaitReply(CountDownLatch latch) throws InterruptedException {
        awaitReply(latch, 1000);
    }

    protected void awaitReply(CountDownLatch latch, int timeoutMillis) throws InterruptedException {
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            fail("waiting for response took too long");
        }
    }

    public static class CountDownLatchChannelHandler extends SimpleChannelInboundHandler<Object> {

        private final CountDownLatch latch;

        private final AtomicInteger responseCount = new AtomicInteger(0);
        private final Map<Integer, Object> responseMap;
        private final Consumer<String> debug;

        public CountDownLatchChannelHandler(CountDownLatch latch, Map<Integer, Object> responseMap, Consumer<String> debug) {
            this.latch = latch;
            this.responseMap = responseMap;
            this.debug = debug;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

            if (msg instanceof TextWebSocketFrame frame) {
                synchronized (latch) {
                    int count = responseCount.incrementAndGet();
                    debug.accept(count + ": " + frame.copy().text());
                    responseMap.put(count, frame.copy());
                    latch.countDown();
                }
            } else if (msg instanceof DatagramPacket datagramPacket) {
                synchronized (latch) {
                    int count = responseCount.incrementAndGet();
                    debug.accept(count + ": " + UdpServer.byteBufferToString(datagramPacket.copy().content().nioBuffer()));
                    responseMap.put(count, datagramPacket.copy());
                    latch.countDown();
                }
            } else {
                throw new RuntimeException("Unexpected message received!");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void assertUdpHelloMessage(HelloReply expectedHelloReply, String sessionId, Character deliminator) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] payload = getMessageBytes("hello", sessionId, deliminator);

            int port = UdpServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();
            java.net.DatagramPacket packet
                    = new java.net.DatagramPacket(payload, payload.length, host, port);
            socket.send(packet);

            byte[] receivedBytes = new byte[expectedHelloReply.toByteArray().length];
            packet = new java.net.DatagramPacket(receivedBytes, receivedBytes.length);
            socket.receive(packet);

            HelloReply helloReply = null;
            try {
                helloReply = HelloReply.parseFrom(packet.getData());
            } catch (Exception ex) {
                fail("There was an issue parsing the DatagramPacket which indicates that you received an unexpected response: " + new String(packet.getData()));
            }
            assertEquals(expectedHelloReply, helloReply);
        }
    }

    public static byte[] getMessageBytes(String address, Character deliminator) {
        return getMessageBytes(address, "guest", deliminator);
    }

    public static byte[] getMessageBytes(String address, String sessionId, Character deliminator) {
        return ArrayUtils.addAll(
                (address + deliminator + sessionId + deliminator).getBytes(),
                HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray()
        );
    }

}
