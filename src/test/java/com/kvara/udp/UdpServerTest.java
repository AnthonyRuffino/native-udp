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
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UdpServerTest {

    @Value("${com.kvara.udp.UdpMessageParser.deliminator:|}")
    Character deliminator;

    @Test
    public void udpServerRawDatagramPacketTest() throws Exception {

        DatagramSocket socket = new DatagramSocket();
        try {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] eventBusAddress = ("hello" + deliminator).getBytes();
            byte[] helloRequest = HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray();
            byte[] payload = ArrayUtils.addAll(eventBusAddress, helloRequest);

            int port = UdpServer.BOUND_PORTS.values().stream().findFirst().orElseThrow();
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
        } finally {
            socket.close();
        }
    }

    @Test
    public void udpServerBootstrappedClientTest() {
        int port = UdpServer.BOUND_PORTS.values().stream().findFirst().orElseThrow();

        HelloReply expectedHelloReply = HelloReply.newBuilder()
                .setMessage("Goodbye Sarlomp")
                .setAdvice("Live long and prosper!")
                .build();

        byte[] messageBytes = ArrayUtils.addAll(
                ("goodbye" + deliminator).getBytes(),
                HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray()
        );

        BootstrappedTestClient<HelloReply> bootstrappedTestClient = new BootstrappedTestClient<>("localhost", port, 50) {
            @Override
            protected HelloReply parse(ByteBuffer byteBuffer) throws Exception {
                return HelloReply.parseFrom(byteBuffer);
            }
        };

        bootstrappedTestClient.shutdownOnComplete = false;
        bootstrappedTestClient.runTest(
                messageBytes, expectedHelloReply
        );

        bootstrappedTestClient.shutdownOnComplete = true;
        bootstrappedTestClient.runTest(
                messageBytes,
                () -> {
                    assertEquals("Live long and prosper!", bootstrappedTestClient.getResponse().getAdvice());
                }
        );
    }
}