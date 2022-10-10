package com.kvara.io.udp;


import com.kvara.HelloReply;
import com.kvara.HelloRequest;
import com.kvara.test.AbstractTestClient;
import com.kvara.test.udp.UdpBootstrappedTestClient;
import io.netty.channel.socket.DatagramPacket;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UdpServerVerticalTest {

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    @Test
    public void udpServerRawDatagramPacketTest() throws Exception {

        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] eventBusAddress = ("hello" + deliminator).getBytes();
            byte[] helloRequest = HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray();
            byte[] payload = ArrayUtils.addAll(eventBusAddress, helloRequest);

            int port = UdpServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();
            java.net.DatagramPacket packet
                    = new java.net.DatagramPacket(payload, payload.length, host, port);
            socket.send(packet);

            HelloReply expectedHelloReply = HelloReply.newBuilder()
                    .setMessage("Hello Sarlomp")
                    .setAdvice("Take care!")
                    .build();

            byte[] receivedBytes = new byte[expectedHelloReply.toByteArray().length];
            packet = new java.net.DatagramPacket(receivedBytes, receivedBytes.length);
            socket.receive(packet);

            HelloReply helloReply = HelloReply.parseFrom(packet.getData());
            assertEquals(expectedHelloReply, helloReply);
        }
    }

    @Test
    public void udpServerBootstrappedClientTest() throws InterruptedException {


        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    DatagramPacket packet = (DatagramPacket) response;
                    var actualHelloReply = HelloReply.parseFrom(packet.content().nioBuffer());
                    assertEquals("Hello Sarlomp", actualHelloReply.getMessage());
                },
                (response) -> {
                    DatagramPacket packet = (DatagramPacket) response;
                    var actualGoodbyeReply = HelloReply.parseFrom(packet.content().nioBuffer());
                    assertEquals("Goodbye Sarlomp", actualGoodbyeReply.getMessage());
                }
        );

        AbstractTestClient udpBootstrappedTestClient = getClient()
                .withAssertions(
                        assertions
                );
        udpBootstrappedTestClient.sendMessage(getMessageBytes("hello", deliminator), 50);
        Thread.sleep(50);
        udpBootstrappedTestClient.sendMessage(getMessageBytes("goodbye", deliminator), 50);
        udpBootstrappedTestClient.checkAssertions(50);
    }

    @Test
    public void udpServerBootstrappedClientCallbackTest() throws InterruptedException {

        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    DatagramPacket packet = (DatagramPacket) response;
                    var actualHelloReply = HelloReply.parseFrom(packet.content().nioBuffer());
                    assertEquals("I'll call you back Sarlomp", actualHelloReply.getMessage());
                },
                (response) -> {
                    DatagramPacket packet = (DatagramPacket) response;
                    var actualGoodbyeReply = HelloReply.parseFrom(packet.content().nioBuffer());
                    assertEquals("Off Thread Message!", actualGoodbyeReply.getMessage());
                }
        );

        AbstractTestClient udpBootstrappedTestClient = getClient()
                .withAssertions(assertions);

        udpBootstrappedTestClient.sendMessage(getMessageBytes("callback", deliminator), 50);
        udpBootstrappedTestClient.checkAssertions(1000);
    }


    @Test
    public void brokenDeliminatorTest() throws InterruptedException {


        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    DatagramPacket packet = (DatagramPacket) response;
                    var actualHelloReply = HelloReply.parseFrom(packet.content().nioBuffer());
                    assertEquals("Unable to parse UDP message", actualHelloReply.getMessage());
                }
        );

        AbstractTestClient udpBootstrappedTestClient = getClient()
                .withAssertions(
                        assertions
                );

        udpBootstrappedTestClient.sendMessage(getMessageBytes("hello", '$'), 50);
        udpBootstrappedTestClient.checkAssertions(50);
    }

    private static UdpBootstrappedTestClient getClient() {
        UdpBootstrappedTestClient client = new UdpBootstrappedTestClient(
                "localhost",
                UdpServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow(),
                50
        );
        client.startup();
        return client;
    }

    private byte[] getMessageBytes(String prefix, Character deliminator) {
        return ArrayUtils.addAll(
                (prefix + deliminator).getBytes(),
                HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray()
        );
    }
}