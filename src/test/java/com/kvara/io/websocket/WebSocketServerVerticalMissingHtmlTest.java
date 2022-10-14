package com.kvara.io.websocket;


import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(WebSocketServerVerticalMissingHtmlTest.BuildTimeValueChangeTestProfile.class)
class WebSocketServerVerticalMissingHtmlTest {

    @Inject
    WebSocketServerVertical webSocketServerVertical;

    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "com.kvara.io.websocket.server.htmlTemplatePath", "static/missing.html",
                    "quarkus.log.category.\"io.worldy.sockiopath.websocket.ui.WebSocketIndexPageHandler\".level", "OFF"
            );
        }
    }

    @Test
    public void webSocketMissingHtmlTest() throws Exception {
        int port = webSocketServerVertical.actualPort();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("WebSocket HTML content was missing.", response.body());
    }
}
