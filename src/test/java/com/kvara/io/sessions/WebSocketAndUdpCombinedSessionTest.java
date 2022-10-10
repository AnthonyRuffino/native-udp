package com.kvara.io.sessions;


import com.kvara.HelloReply;
import com.kvara.io.websocket.WebSocketServerVertical;
import com.kvara.test.AbstractTestClient;
import com.kvara.test.udp.UdpBootstrappedTestClient;
import com.kvara.test.websocket.WebSocketBootstrappedTestClient;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class WebSocketAndUdpCombinedSessionTest {

    private static final String SESSION_TOKEN_PREFIX = "session|";

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    @Test
    public void webSocketAndUdpCombinedSessionTest() throws Exception {

        List<AbstractTestClient.Assertion> assertions = List.of(
                (response) -> {
                    TextWebSocketFrame textFrame = ((TextWebSocketFrame) response).copy();
                    assertTrue(textFrame.text().startsWith(SESSION_TOKEN_PREFIX));
                }
        );

        AbstractTestClient client = getClient()
                .withAssertions(
                        assertions
                );
        client.startup();

        client.sendMessage(new TextWebSocketFrame("join"), 50);
        client.checkAssertions(50);


        TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) client.getAssertions().get(1l).getResponse();
        String sessionId = extractSessionId(textWebSocketFrame.text());


        HelloReply expectedHelloReply = HelloReply.newBuilder()
                .setMessage("Hello Sarlomp")
                .setAdvice("Take care!")
                .build();

        UdpBootstrappedTestClient.assertHelloMessage(expectedHelloReply, sessionId, deliminator);

        HelloReply expectedHFailedValidationReply = HelloReply.newBuilder()
                .setMessage("You did not send a valid session ID.")
                .setAdvice("Connect with WebSockets to and send a 'join' message to get a session id!")
                .build();

        UdpBootstrappedTestClient.assertHelloMessage(expectedHFailedValidationReply, "erroneous", deliminator);
    }

    private String extractSessionId(String sessionResponse) {

        return sessionResponse.substring(sessionResponse.indexOf(SESSION_TOKEN_PREFIX) + SESSION_TOKEN_PREFIX.length());
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