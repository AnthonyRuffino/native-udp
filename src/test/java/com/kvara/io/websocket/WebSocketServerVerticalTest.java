package com.kvara.io.websocket;

import com.kvara.AbstractTest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.quarkus.test.junit.QuarkusTest;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WebSocketServerVerticalTest extends AbstractTest {

    @Inject
    WebSocketServerVertical webSocketServerVertical;

    @Test
    public void webSocketSessionTest() throws Exception {

        int test1Count = 2;
        CountDownLatch latch1 = new CountDownLatch(test1Count);
        Map<Integer, Object> responseMap1 = new HashMap<>();
        BootstrappedWebSocketClient client1 = getClient(latch1, responseMap1);
        client1.startup();


        sendMessage(client1.getChannel(), "join");
        Thread.sleep(2L);
        sendMessage(client1.getChannel(), "test", 50);
        awaitReply(latch1);

        TextWebSocketFrame textWebSocketFrame1_1 = (TextWebSocketFrame) responseMap1.get(1);
        assertTrue(textWebSocketFrame1_1.text().startsWith("session|"));

        TextWebSocketFrame textWebSocketFrame1_2 = (TextWebSocketFrame) responseMap1.get(2);
        assertEquals("test", textWebSocketFrame1_2.text());

        Thread.sleep(2L);


        int test2Count = 3;
        CountDownLatch latch2 = new CountDownLatch(test2Count);
        Map<Integer, Object> responseMap2 = new HashMap<>();
        BootstrappedWebSocketClient client2 = getClient(latch2, responseMap2);
        client2.startup();

        sendMessage(client2.getChannel(), "join");
        Thread.sleep(2L);
        sendMessage(client2.getChannel(), "test", 50);

        Thread.sleep(2L);
        sendMessage(client1.getChannel(), "Hi there", 50);
        Thread.sleep(2L);

        awaitReply(latch2);

        TextWebSocketFrame textWebSocketFrame2_1 = (TextWebSocketFrame) responseMap2.get(1);
        assertTrue(textWebSocketFrame2_1.text().startsWith("session|"));

        TextWebSocketFrame textWebSocketFrame2_2 = (TextWebSocketFrame) responseMap2.get(2);
        assertEquals("test", textWebSocketFrame2_2.text());

        TextWebSocketFrame textWebSocketFrame2_3 = (TextWebSocketFrame) responseMap2.get(3);
        assertThat(textWebSocketFrame2_3.text(), StringContainsInOrder.stringContainsInOrder("Hi there"));
    }

    private BootstrappedWebSocketClient getClient(CountDownLatch latch, Map<Integer, Object> responseMap) {
        return new BootstrappedWebSocketClient(
                "localhost",
                webSocketServerVertical.actualPort(),
                "/websocket",
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                null,
                500,
                500
        );
    }
}
