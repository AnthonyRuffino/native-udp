package com.kvara.udp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public class UdpMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Vertx vertx;
    private final Function<ByteBuffer, Optional<UdpParsedMessage>> udpMessageParser;
    private final boolean flush;


    public UdpMessageHandler(
            Vertx vertx,
            Function<ByteBuffer, Optional<UdpParsedMessage>> udpMessageParser,
            boolean flush
    ) {
        this.vertx = vertx;
        this.udpMessageParser = udpMessageParser;
        this.flush = flush;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        System.out.println(packet);
        udpMessageParser.apply(packet.content().nioBuffer())
                .ifPresentOrElse(
                        udpParsedMessage -> process(udpParsedMessage, context, packet),
                        () -> logError(context, packet)
                );
    }

    private void logError(ChannelHandlerContext context, DatagramPacket packet) {
        System.err.println("Unable to parse UDP message");
        context.write(new DatagramPacket(Unpooled.copiedBuffer("Something went wrong!".getBytes()), packet.sender()));

    }

    private void process(UdpParsedMessage udpParsedMessage, ChannelHandlerContext context, DatagramPacket packet) {
        forward(udpParsedMessage)
                .onItem()
                .transform(message ->
                        new DatagramPacket(Unpooled.copiedBuffer(message.body()), packet.sender())
                )
                .subscribe()
                .with(datagramPacket -> {
                    context.write(datagramPacket);
                    if (flush) {
                        context.flush();
                    }
                });
    }

    private Uni<Message<byte[]>> forward(UdpParsedMessage udpParsedMessage) {
        return vertx.eventBus().request(udpParsedMessage.address(), udpParsedMessage.data());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        // We don't close the channel because we can keep serving requests.
    }
}
