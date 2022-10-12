package com.kvara.io.websocket;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.shareddata.SharedData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketFrameHandlerTest {
    @Test
    void channelRead0() {
        Vertx vertx = Mockito.mock(Vertx.class);
        SharedData sharedData = Mockito.mock(SharedData.class);
        Mockito.when(vertx.sharedData()).thenReturn(sharedData);
        assertThrows(UnsupportedOperationException.class, () -> {
            new WebSocketFrameHandler(vertx).channelRead0(null, "string");
        });
    }
}
