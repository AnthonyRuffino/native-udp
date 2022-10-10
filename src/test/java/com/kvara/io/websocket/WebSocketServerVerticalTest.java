package com.kvara.io.websocket;


import com.kvara.test.AbstractTestClient;
import com.kvara.test.websocket.WebSocketBootstrappedTestClient;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WebSocketServerVerticalTest {

    @Test
    public void webSocketSessionTest() throws Exception {

        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    TextWebSocketFrame textFrame = (TextWebSocketFrame) response;
                    assertTrue(textFrame.text().startsWith("session|"));
                },
                (response) -> {
                    TextWebSocketFrame textFrame = (TextWebSocketFrame) response;
                    assertEquals("test", textFrame.text());
                }
        );

        AbstractTestClient client = getClient()
                .withAssertions(
                        assertions
                );
        client.setDebug(false);
        client.startup();

        client.sendMessage(new TextWebSocketFrame("join"), 50);
        client.sendMessage(new TextWebSocketFrame("test"), 50);
        client.checkAssertions(50);

    }


    @Test
    public void webSocketSessionJoinTest() throws Exception {

        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    TextWebSocketFrame textFrame = (TextWebSocketFrame) response;
                    assertTrue(textFrame.text().startsWith("session|"));
                }
        );

        AbstractTestClient client1 = getClient()
                .withAssertions(
                        assertions
                );
        client1.setDebug(false);
        client1.startup();

        WebSocketFrame frame1 = new TextWebSocketFrame("join");
        client1.sendMessage(frame1, 50);
        client1.checkAssertions(50);

        if (!client1.shutdown().await(5000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client1 took too long to shutdown.");
        }

        AbstractTestClient client2 = getClient()
                .withAssertions(
                        assertions
                );
        client2.setDebug(false);
        client2.startup();

        WebSocketFrame frame2 = new TextWebSocketFrame("join");
        client2.sendMessage(frame2, 50);
        client2.checkAssertions(50);

    }

    private static WebSocketBootstrappedTestClient getClient() {
        return new WebSocketBootstrappedTestClient(
                "localhost",
                WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow(),
                500, "/websocket",
                false,
                500
        );
    }

}