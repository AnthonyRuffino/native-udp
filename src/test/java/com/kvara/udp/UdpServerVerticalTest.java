package com.kvara.udp;


import com.kvara.HelloReply;
import com.kvara.HelloRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UdpServerVerticalTest {

    @Value("${com.kvara.MessageParser.deliminator:|}")
    Character deliminator;

    @Test
    public void udpServerRawDatagramPacketTest() throws Exception {

        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] eventBusAddress = ("hello" + deliminator).getBytes();
            byte[] helloRequest = HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray();
            byte[] payload = ArrayUtils.addAll(eventBusAddress, helloRequest);

            int port = UdpServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();
            DatagramPacket packet
                    = new DatagramPacket(payload, payload.length, host, port);
            socket.send(packet);

            HelloReply expectedHelloReply = HelloReply.newBuilder()
                    .setMessage("Hello Sarlomp")
                    .setAdvice("Take care!")
                    .build();

            byte[] receivedBytes = new byte[expectedHelloReply.toByteArray().length];
            packet = new DatagramPacket(receivedBytes, receivedBytes.length);
            socket.receive(packet);

            HelloReply helloReply = HelloReply.parseFrom(packet.getData());
            assertEquals(expectedHelloReply, helloReply);
        }
    }

    @Test
    public void udpServerBootstrappedClientTest() throws InterruptedException {


        List<BootstrappedTestClient.Assertion> assertions = List.of(
                (response) -> {
                    var actualHelloReply = HelloReply.parseFrom(response.content().nioBuffer());
                    assertEquals("Hello Sarlomp", actualHelloReply.getMessage());
                },
                (response) -> {
                    var actualGoodbyeReply = HelloReply.parseFrom(response.content().nioBuffer());
                    assertEquals("Goodbye Sarlomp", actualGoodbyeReply.getMessage());
                }
        );

        BootstrappedTestClient bootstrappedTestClient = getClient()
                .withAssertions(
                        assertions
                );

        bootstrappedTestClient.sendMessage(getMessageBytes("hello", deliminator), 50);
        Thread.sleep(50);
        bootstrappedTestClient.sendMessage(getMessageBytes("goodbye", deliminator), 50);
        bootstrappedTestClient.checkAssertions(50);
    }

    @Test
    public void udpServerBootstrappedClientCallbackTest() throws InterruptedException {

        List<BootstrappedTestClient.Assertion> assertions = List.of(
                (response) -> {
                    var actualHelloReply = HelloReply.parseFrom(response.content().nioBuffer());
                    assertEquals("I'll call you back Sarlomp", actualHelloReply.getMessage());
                },
                (response) -> {
                    var actualGoodbyeReply = HelloReply.parseFrom(response.content().nioBuffer());
                    assertEquals("Off Thread Message!", actualGoodbyeReply.getMessage());
                }
        );

        BootstrappedTestClient bootstrappedTestClient = getClient()
                .withAssertions(assertions);
        bootstrappedTestClient.debug = false;

        bootstrappedTestClient.sendMessage(getMessageBytes("callback", deliminator), 50);
        bootstrappedTestClient.checkAssertions(1000);
    }


    @Test
    public void brokenDeliminatorTest() throws InterruptedException {


        List<BootstrappedTestClient.Assertion> assertions = List.of(
                (response) -> {
                    var actualHelloReply = HelloReply.parseFrom(response.content().nioBuffer());
                    assertEquals("Unable to parse UDP message", actualHelloReply.getMessage());
                }
        );

        BootstrappedTestClient bootstrappedTestClient = getClient()
                .withAssertions(
                        assertions
                );

        bootstrappedTestClient.sendMessage(getMessageBytes("hello", '$'), 50);
        bootstrappedTestClient.checkAssertions(50);
    }

    private static BootstrappedTestClient getClient() {
        return new BootstrappedTestClient(
                "localhost",
                UdpServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow(),
                50
        );
    }

    private byte[] getMessageBytes(String prefix, Character deliminator) {
        return ArrayUtils.addAll(
                (prefix + deliminator).getBytes(),
                HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray()
        );
    }
}