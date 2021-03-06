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

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class UdpServerTest {

    @Value("${com.kvara.udp.UdpMessageParser.deliminator:|}")
    Character deliminator;

    @Test
    public void usdServerTest() throws Exception {

        DatagramSocket socket = new DatagramSocket();
        try {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] eventBusAddress = ("hello" + deliminator).getBytes();
            byte[] helloRequest = HelloRequest.newBuilder().setName("Sarlomp").build().toByteArray();
            byte[] payload = ArrayUtils.addAll(eventBusAddress, helloRequest);

            DatagramPacket packet
                    = new DatagramPacket(payload, payload.length, host, 8888);
            socket.send(packet);

            HelloReply expectedHelloReply = HelloReply.newBuilder()
                    .setMessage("Hello Sarlomp")
                    .setAdvice("care")
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

}