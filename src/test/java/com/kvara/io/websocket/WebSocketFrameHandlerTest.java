package com.kvara.io.websocket;

import io.vertx.mutiny.core.Vertx;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketFrameHandlerTest {
    @Test
    void channelRead0() {
        Vertx vertx = Mockito.mock(Vertx.class);
        io.vertx.core.Vertx vertxDelegate = Mockito.mock(io.vertx.core.Vertx.class);
        io.vertx.core.shareddata.SharedData sharedData = Mockito.mock(io.vertx.core.shareddata.SharedData.class);
        Mockito.when(vertxDelegate.sharedData()).thenReturn(sharedData);
        Mockito.when(vertx.getDelegate()).thenReturn(vertxDelegate);
        assertThrows(UnsupportedOperationException.class, () -> {
            new WebSocketFrameHandler(vertx).channelRead0(null, "string");
        });
    }
}
