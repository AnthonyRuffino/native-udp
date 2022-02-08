package com.kvara.net.message;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
public class MessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Autowired
    private EventBus eventBus;

    @Value("${com.kvara.udp.UdpServer.flush}")
    private boolean flush;

    @Autowired
    @Qualifier("messageParser")
    private Function<ByteBuffer, Optional<ParsedMessage>> messageParser;

    @ConsumeEvent("broadcast")
    public byte[] broadcast(byte[] message) {
        return null;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        System.out.println(packet);
        messageParser.apply(packet.content().nioBuffer())
                .ifPresentOrElse(
                        parsedMessage -> process(parsedMessage, context, packet),
                        () -> logError(context, packet)
                );

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    private void logError(ChannelHandlerContext context, DatagramPacket packet) {
        System.err.println("Unable to parse UDP message");
        context.write(new DatagramPacket(Unpooled.copiedBuffer("Something went wrong!".getBytes()), packet.sender()));

    }

    private void process(ParsedMessage parsedMessage, ChannelHandlerContext context, DatagramPacket packet) {
        forward(packet.sender(), parsedMessage)
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

    private Uni<Message<byte[]>> forward(InetSocketAddress sender, ParsedMessage parsedMessage) {
        return eventBus.request(parsedMessage.address(), parsedMessage.data());
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
