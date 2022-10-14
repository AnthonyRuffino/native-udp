package com.kvara.io.udp;

import com.kvara.HelloReply;
import com.kvara.io.ParsedMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.shareddata.LocalMap;
import io.worldy.sockiopath.websocket.session.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public class UdpMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(UdpMessageHandler.class);

    private final Vertx vertx;

    private final LocalMap<String, WebSocketSession> sessionsShared;
    private final Function<ByteBuffer, Optional<ParsedMessage>> messageParser;
    private ChannelHandlerContext ctx;

    public UdpMessageHandler(
            Vertx vertx,
            Function<ByteBuffer, Optional<ParsedMessage>> messageParser
    ) {
        this.vertx = vertx;
        this.messageParser = messageParser;
        this.sessionsShared = vertx.sharedData().getLocalMap("sessions");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        messageParser.apply(packet.content().nioBuffer())
                .ifPresentOrElse(
                        parsedMessage -> process(parsedMessage, context, packet),
                        () -> logError(context, packet)
                );
    }

    private void logError(ChannelHandlerContext context, DatagramPacket packet) {
        context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(
                HelloReply.newBuilder()
                        .setMessage("Unable to parse UDP message")
                        .build()
                        .toByteArray()
        ), packet.sender()));
    }

    private void process(ParsedMessage parsedMessage, ChannelHandlerContext context, DatagramPacket packet) {
        InetSocketAddress sender = packet.sender();

        if (!"guest".equals(parsedMessage.sessionId()) && !sessionsShared.containsKey(parsedMessage.sessionId())) {
            HelloReply failedValidationReply = HelloReply.newBuilder()
                    .setMessage("You did not send a valid session ID.")
                    .setAdvice("Connect with WebSockets to and send a 'join' message to get a session id!")
                    .build();
            context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(failedValidationReply.toByteArray()), sender));
            return;
        }

        forward(parsedMessage, sender)
                .onItem()
                .transform(message ->
                        new DatagramPacket(Unpooled.copiedBuffer(message.body()), sender)
                )
                .subscribe()
                .with(context::writeAndFlush);
    }

    private Uni<Message<byte[]>> forward(ParsedMessage parsedMessage, InetSocketAddress sender) {
        if (parsedMessage.address().equals("callback")) {
            CallbackThread t = new CallbackThread(ctx, sender, 100);
            t.start();
        }
        return vertx.eventBus().request(parsedMessage.address(), parsedMessage);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error handling Udp connection: " + cause.getMessage(), cause);
        // We don't close the channel because we can keep serving requests.
    }
}
