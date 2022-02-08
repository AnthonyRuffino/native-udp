package com.kvara.net;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.kvara.HelloRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ChatTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/websocket/firstUri")
    URI firstUri;

    @Test
    public void testWebsocketChat() throws Exception {
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, firstUri)) {
//            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
//            Assertions.assertEquals("User stu joined", MESSAGES.poll(10, TimeUnit.SECONDS));
            session.getAsyncRemote().sendText("hello world");
//            Assertions.assertEquals(">> stu: hello world", MESSAGES.poll(10, TimeUnit.SECONDS));
            System.out.println(MESSAGES.poll(10, TimeUnit.SECONDS));
            System.out.println(MESSAGES.poll(10, TimeUnit.SECONDS));

        }

    }

    @ClientEndpoint
    public static class Client {

        @OnOpen
        public void open(Session session) {
            MESSAGES.add("CONNECT");
            session.getAsyncRemote().sendBinary(
                    HelloRequest.newBuilder().setName("joe").build().toByteString().asReadOnlyByteBuffer()
            );
            // Send a message to indicate that we are ready,
            // as the message handler may not be registered immediately after this callback.
            //session.getAsyncRemote().sendText(new String(HelloRequest.newBuilder().setName("joe").build().toByteArray()));
        }

        @OnMessage
        void message(String msg) {
            MESSAGES.add(msg);
        }

    }

}
