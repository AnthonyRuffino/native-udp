package com.kvara.io.websocket;

import com.kvara.io.websocket.session.WebSocketSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.mutiny.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    private static final String TEXT_COMMAND_JOIN = "join";

    private static final String DELIMINATOR = "|";
    private static final String TEXT_RESPONSE_PART_SESSION = "session" + DELIMINATOR;
    private final Vertx vertx;
    private final LocalMap<String, WebSocketSession> sessionsShared;

    public WebSocketFrameHandler(Vertx vertx) {
        this.vertx = vertx;
        this.sessionsShared = this.vertx.getDelegate().sharedData().getLocalMap("sessions");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object frame) {

        if (frame instanceof TextWebSocketFrame textFrame) {
            String textMessage = textFrame.text();
            logger.debug("{} received {}", ctx.channel(), textMessage);
            logger.debug("sessions {}", sessionsShared.size());

            String sessionId = getChannelId(ctx.channel());
            String sessionShortId = getChannelShortId(ctx.channel());
            if (TEXT_COMMAND_JOIN.equals(textMessage)) {
                createSession(ctx);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(TEXT_RESPONSE_PART_SESSION + sessionId));
            } else {
                sessionsShared.forEach((key, value) -> {
                    boolean isSameSession = key.equals(sessionId);
                    String prefix = isSameSession ? "" : (sessionShortId + ": ");
                    value.getContext().writeAndFlush(new TextWebSocketFrame(prefix + textMessage));
                });
            }
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    private void createSession(ChannelHandlerContext ctx) {
        String sessionId = getChannelId(ctx.channel());
        logger.debug("createSession {}", sessionId);
        sessionsShared.put(sessionId, new WebSocketSession(ctx));
    }

    private void removeSession(ChannelHandlerContext ctx) {
        String sessionId = getChannelId(ctx.channel());
        logger.debug("removeSession {}", sessionId);
        synchronized (sessionsShared) {
            sessionsShared.remove(sessionId);
        }
    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        removeSession(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    private String getChannelId(Channel channel) {
        return channel.id().asLongText();
    }

    private String getChannelShortId(Channel channel) {
        return channel.id().asShortText();
    }
}
