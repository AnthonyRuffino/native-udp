package com.kvara.test.websocket;

/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import com.kvara.test.AbstractTestClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;


public final class WebSocketBootstrappedTestClient extends AbstractTestClient {


    private final String path;
    private final boolean ssl;
    private final int handshakeTimeout;

    public WebSocketBootstrappedTestClient(String host, int port, int connectTimeout, String path, boolean ssl, int handshakeTimeout) {
        super(host, port, connectTimeout);
        this.path = path;
        this.ssl = ssl;
        this.handshakeTimeout = handshakeTimeout;
    }

    @Override
    public void startup() {
        workGroup = new NioEventLoopGroup();

        try {
            String scheme = ssl ? "wss://" : "ws://";

            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }

            URI uri = new URI(scheme + host + path);

            final CountDownWebSocketClientHandler handler =
                    new CountDownWebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()),
                            latch,
                            assertions,
                            this::debugMessage);


            Bootstrap b = new Bootstrap();
            b.group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    handler
                            );
                        }
                    });

            ChannelFuture channelFuture = b.connect(uri.getHost(), port);
            if (!channelFuture.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Client took too long to connect");
            }

            if (!handler.handshakeFuture().await(handshakeTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Handshake took too long");
            }

            this.channel = channelFuture.channel();
        } catch (URISyntaxException | SSLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
