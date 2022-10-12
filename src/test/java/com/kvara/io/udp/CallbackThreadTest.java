package com.kvara.io.udp;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;

class CallbackThreadTest {

    @Test
    void failedRun() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        new CallbackThread(ctx, sender, 100).sendCallback();
        Mockito.verify(ctx, Mockito.never()).close();
    }
}
