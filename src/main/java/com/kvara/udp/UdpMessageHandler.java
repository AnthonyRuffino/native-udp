package com.kvara.udp;

import com.kvara.HelloReply;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public class UdpMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Vertx vertx;
    private final Function<ByteBuffer, Optional<UdpParsedMessage>> udpMessageParser;
    private final boolean flush;
    private ChannelHandlerContext ctx;

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
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        udpMessageParser.apply(packet.content().nioBuffer())
                .ifPresentOrElse(
                        udpParsedMessage -> process(udpParsedMessage, context, packet),
                        () -> logError(context, packet)
                );
    }

    private void logError(ChannelHandlerContext context, DatagramPacket packet) {
        context.write(new DatagramPacket(Unpooled.copiedBuffer(
                HelloReply.newBuilder()
                        .setMessage("Unable to parse UDP message")
                        .build()
                        .toByteArray()
        ), packet.sender()));
    }

    private void process(UdpParsedMessage udpParsedMessage, ChannelHandlerContext context, DatagramPacket packet) {
        InetSocketAddress sender = packet.sender();
        forward(udpParsedMessage, sender)
                .onItem()
                .transform(message ->
                        new DatagramPacket(Unpooled.copiedBuffer(message.body()), sender)
                )
                .subscribe()
                .with(datagramPacket -> {
                    context.write(datagramPacket);
                    if (flush) {
                        context.flush();
                    }
                });
    }

    private Uni<Message<byte[]>> forward(UdpParsedMessage udpParsedMessage, InetSocketAddress sender) {
        if (udpParsedMessage.address().equals("callback")) {
            CallbackThread t = new CallbackThread(ctx, sender, 100);
            t.start();
        }
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
