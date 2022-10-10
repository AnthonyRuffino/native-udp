package com.kvara.io.udp;


import com.kvara.HelloReply;
import com.kvara.test.AbstractTestClient;
import com.kvara.test.udp.UdpBootstrappedTestClient;
import io.netty.channel.socket.DatagramPacket;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static com.kvara.test.udp.UdpBootstrappedTestClient.getMessageBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UdpServerVerticalTest {

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    @Test
    public void udpServerRawDatagramPacketTest() throws Exception {
        HelloReply expectedHelloReply = HelloReply.newBuilder()
                .setMessage("Hello Sarlomp")
                .setAdvice("Take care!")
                .build();

        UdpBootstrappedTestClient.assertHelloMessage(expectedHelloReply, "guest", deliminator);
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
}