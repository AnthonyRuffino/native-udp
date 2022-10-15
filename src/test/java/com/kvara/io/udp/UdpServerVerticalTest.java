package com.kvara.io.udp;


import com.google.protobuf.InvalidProtocolBufferException;
import com.kvara.AbstractTest;
import com.kvara.HelloReply;
import com.kvara.io.SharedSockiopathSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.mutiny.core.Vertx;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.client.BootstrappedUdpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class UdpServerVerticalTest extends AbstractTest {

    @Inject
    UdpServerVertical udpServerVertical;


    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;


    @Inject
    Vertx vertx;

    @Test
    public void udpServerRawDatagramPacketTest() throws Exception {
        HelloReply expectedHelloReply = HelloReply.newBuilder()
                .setMessage("Hello Sarlomp")
                .setAdvice("Take care!")
                .build();

        LocalMap<String, SockiopathSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        sessionMap.put("guest", new SharedSockiopathSession(Mockito.mock(ChannelHandlerContext.class)));

        assertUdpHelloMessage(udpServerVertical.actualPort(), expectedHelloReply, "guest", deliminator);
    }

    @Test
    public void udpServerBootstrappedClientTest() throws InterruptedException, InvalidProtocolBufferException {

        LocalMap<String, SockiopathSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        sessionMap.put("guest", new SharedSockiopathSession(Mockito.mock(ChannelHandlerContext.class)));

        int testCount = 2;
        CountDownLatch latch = new CountDownLatch(testCount);
        Map<Integer, Object> responseMap = new HashMap<>();

        BootstrappedUdpClient client = getClient(latch, responseMap);

        client.startup();

        sendMessage(client.getChannel(), getMessageBytes("hello", deliminator), 200);
        Thread.sleep(500);

        sendMessage(client.getChannel(), getMessageBytes("goodbye", deliminator), 200);

        awaitReply(latch, 1000);

        var actualHelloReply = HelloReply.parseFrom(((DatagramPacket) responseMap.get(1)).content().nioBuffer());
        assertEquals("Hello Sarlomp", actualHelloReply.getMessage());


        var actualGoodbyeReply = HelloReply.parseFrom(((DatagramPacket) responseMap.get(2)).content().nioBuffer());
        assertEquals("Goodbye Sarlomp", actualGoodbyeReply.getMessage());

    }

    @Test
    public void udpServerBootstrappedClientCallbackTest() throws InterruptedException, InvalidProtocolBufferException {

        LocalMap<String, SockiopathSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        sessionMap.put("guest", new SharedSockiopathSession(Mockito.mock(ChannelHandlerContext.class)));

        int testCount = 2;
        CountDownLatch latch = new CountDownLatch(testCount);
        Map<Integer, Object> responseMap = new HashMap<>();

        BootstrappedUdpClient client = getClient(latch, responseMap);
        client.startup();

        sendMessage(client.getChannel(), getMessageBytes("callback", deliminator), 200);

        awaitReply(latch, 1000);

        HelloReply actualCallbackImmediateReply = HelloReply.parseFrom(((DatagramPacket) responseMap.get(1)).content().nioBuffer());
        assertEquals("I'll call you back Sarlomp", actualCallbackImmediateReply.getMessage());


        HelloReply actualCallbackDelayedResponse = HelloReply.parseFrom(((DatagramPacket) responseMap.get(2)).content().nioBuffer());
        assertEquals("Off Thread Message!", actualCallbackDelayedResponse.getMessage());
    }


    @Test
    public void brokenDeliminatorTest() throws InterruptedException, InvalidProtocolBufferException {

        LocalMap<String, SockiopathSession> sessionMap = vertx.getDelegate().sharedData().getLocalMap("sessions");
        sessionMap.put("guest", new SharedSockiopathSession(Mockito.mock(ChannelHandlerContext.class)));

        int testCount = 1;
        CountDownLatch latch = new CountDownLatch(testCount);
        Map<Integer, Object> responseMap = new HashMap<>();

        BootstrappedUdpClient client = getClient(latch, responseMap);
        client.startup();

        sendMessage(client.getChannel(), getMessageBytes("hello", '$'), 200);

        awaitReply(latch, 500, true);
        assertNull(responseMap.get(1));
    }

    private BootstrappedUdpClient getClient(CountDownLatch latch, Map<Integer, Object> responseMap) {
        return getClient(latch, responseMap, false);
    }

    ;

    private BootstrappedUdpClient getClient(CountDownLatch latch, Map<Integer, Object> responseMap, boolean debug) {
        return new BootstrappedUdpClient(
                "localhost",
                udpServerVertical.actualPort(),
                new AbstractTest.CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                    if (debug) {
                        System.out.println("!!!!! DEBUG !!!!! [ " + message + " ]");
                    }
                }),
                500
        );
    }
}
