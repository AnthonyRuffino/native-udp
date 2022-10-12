package com.kvara.io.websocket;

import com.kvara.AbstractTest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.quarkus.test.junit.QuarkusTest;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WebSocketServerVerticalTest extends AbstractTest {

    @Test
    public void webSocketSessionTest() throws Exception {

        int testCount = 2;
        CountDownLatch latch = new CountDownLatch(testCount);
        Map<Integer, Object> responseMap = new HashMap<>();

        BootstrappedWebSocketClient client = new BootstrappedWebSocketClient(
                "localhost",
                WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow(),
                "/websocket",
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                null,
                500,
                500
        );
        client.startup();

        sendMessage(client.getChannel(), "join");
        Thread.sleep(2L);
        sendMessage(client.getChannel(), "test", 50);
        awaitReply(latch);

        TextWebSocketFrame textWebSocketFrame1 = (TextWebSocketFrame) responseMap.get(1);
        assertTrue(textWebSocketFrame1.text().startsWith("session|"));

        TextWebSocketFrame textWebSocketFrame2 = (TextWebSocketFrame) responseMap.get(2);
        assertEquals("test", textWebSocketFrame2.text());
    }

}