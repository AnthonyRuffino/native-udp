package com.kvara.io.sessions;


import com.kvara.AbstractTest;
import com.kvara.HelloReply;
import com.kvara.io.udp.UdpServerVertical;
import com.kvara.io.websocket.WebSocketServerVertical;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.quarkus.test.junit.QuarkusTest;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WebSocketAndUdpCombinedSessionTest extends AbstractTest {

    private static final String SESSION_TOKEN_PREFIX = "session|";

    @Inject
    UdpServerVertical udpServerVertical;

    @Inject
    WebSocketServerVertical webSocketServerVertical;

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    @Test
    public void webSocketAndUdpCombinedSessionTest() throws Exception {

        int testCount = 1;
        CountDownLatch latch = new CountDownLatch(testCount);
        Map<Integer, Object> responseMap = new HashMap<>();

        BootstrappedWebSocketClient client = new BootstrappedWebSocketClient(
                "localhost",
                webSocketServerVertical.actualPort(),
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

        TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) responseMap.get(1);
        assertTrue(textWebSocketFrame.text().startsWith(SESSION_TOKEN_PREFIX));

        String sessionId = extractSessionId(textWebSocketFrame.text());

        HelloReply expectedHelloReply = HelloReply.newBuilder()
                .setMessage("Hello Sarlomp")
                .setAdvice("Take care!")
                .build();

        int port = udpServerVertical.actualPort();
        assertUdpHelloMessage(port, expectedHelloReply, sessionId, deliminator);

        HelloReply expectedHFailedValidationReply = HelloReply.newBuilder()
                .setMessage("You did not send a valid session ID.")
                .setAdvice("Connect with WebSockets to and send a 'join' message to get a session id!")
                .build();

        assertUdpHelloMessage(port, expectedHFailedValidationReply, "erroneous", deliminator);
    }

    private String extractSessionId(String sessionResponse) {

        return sessionResponse.substring(sessionResponse.indexOf(SESSION_TOKEN_PREFIX) + SESSION_TOKEN_PREFIX.length());
    }
}
