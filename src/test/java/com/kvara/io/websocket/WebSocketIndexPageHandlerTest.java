package com.kvara.io.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketIndexPageHandlerTest {

    @Test
    void channelRead0() throws Exception {
        testChannelRead0(new HeadersMultiMap());
    }

    @Test
    void channelRead0IsNotKeepAlive() throws Exception {
        testChannelRead0(HeadersMultiMap.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE));
    }

    void testChannelRead0(HttpHeaders headers) throws Exception {
        FullHttpRequest req = Mockito.mock(FullHttpRequest.class);
        DecoderResult decoderResult = Mockito.mock(DecoderResult.class);
        Mockito.when(decoderResult.isSuccess()).thenReturn(false);
        Mockito.when(req.decoderResult()).thenReturn(decoderResult);
        Mockito.when(req.headers()).thenReturn(headers);
        Mockito.when(req.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);


        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
        ChannelFuture writeFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(channel.writeAndFlush(org.mockito.Mockito.isA(FullHttpResponse.class))).thenReturn(writeFuture);

        new WebSocketIndexPageHandler("", "")
                .channelRead0(ctx, req);

        Mockito.verify(writeFuture, Mockito.atLeastOnce()).addListener(ChannelFutureListener.CLOSE);
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.isA(DefaultFullHttpResponse.class));
    }

    @Test
    void exceptionCaught() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        WebSocketIndexPageHandler webSocketIndexPageHandler = new WebSocketIndexPageHandler("", "");
        webSocketIndexPageHandler.exceptionCaught(ctx, new RuntimeException("Mocked exception"));

        Mockito.verify(ctx, Mockito.times(1)).close();
    }

    @Test
    void getWebSocketLocationForSsl() {
        ChannelPipeline cp = Mockito.mock(ChannelPipeline.class);
        SslHandler handler = Mockito.mock(SslHandler.class);
        Mockito.when(cp.get(SslHandler.class)).thenReturn(handler);

        HttpRequest req = Mockito.mock(HttpRequest.class);
        Mockito.when(req.headers()).thenReturn(HeadersMultiMap.headers().add(HttpHeaderNames.HOST, "secureland"));

        String expected = "wss://secureland/websocketz";
        String actual = WebSocketIndexPageHandler.getWebSocketLocation(cp, req, "/websocketz");

        assertEquals(expected, actual);
    }

    @Test
    void getWebSocketLocationForNonSsl() {
        ChannelPipeline cp = Mockito.mock(ChannelPipeline.class);
        Mockito.when(cp.get(SslHandler.class)).thenReturn(null);

        HttpRequest req = Mockito.mock(HttpRequest.class);
        Mockito.when(req.headers()).thenReturn(HeadersMultiMap.headers().add(HttpHeaderNames.HOST, "insecureland"));

        String expected = "ws://insecureland/websocketz";
        String actual = WebSocketIndexPageHandler.getWebSocketLocation(cp, req, "/websocketz");

        assertEquals(expected, actual);
    }
}
