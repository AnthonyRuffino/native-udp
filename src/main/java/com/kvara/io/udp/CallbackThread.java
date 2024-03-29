package com.kvara.io.udp;

import com.kvara.HelloReply;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class CallbackThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(CallbackThread.class);

    private final ChannelHandlerContext channelHandlerContext;
    private final InetSocketAddress recipient;

    final int delay;

    public CallbackThread(ChannelHandlerContext channelHandlerContext, InetSocketAddress recipient, int delay) {
        this.channelHandlerContext = channelHandlerContext;
        this.recipient = recipient;
        this.delay = delay;
    }

    public void run() {
        sendCallback();
    }

    void sendCallback() {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            Thread.sleep(delay);

            HelloReply expectedHelloReply = HelloReply.newBuilder()
                    .setMessage("Off Thread Message!")
                    .setAdvice("This message was sent in another thread!")
                    .build();

            byte[] bytes = expectedHelloReply.toByteArray();
            var length = bytes.length;

            datagramSocket.connect(recipient);
            java.net.DatagramPacket sendPacket = new java.net.DatagramPacket(bytes, length);
            datagramSocket.send(sendPacket);
            channelHandlerContext.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(bytes), recipient));
        } catch (Exception e) {
            logger.error("Error handling Udp connection during callback: " + e.getMessage(), e);
        }
    }
}
