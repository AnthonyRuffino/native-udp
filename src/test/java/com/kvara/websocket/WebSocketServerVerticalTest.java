package com.kvara.websocket;


import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class WebSocketServerVerticalTest {

    @Test
    public void webSocketSessionTest() throws Exception {
        int port = WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();
        WebSocketClient.connect("localhost", port, "/websocket", false);
    }

    @Test
    public void webSocketHtmlTest() throws Exception {
        int port = WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port))
                .header("accept", "application/json")
                .build();

        String expectedHtml = EXPECTED_HTML.formatted(port);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(StringUtils.trimAllWhitespace(expectedHtml), StringUtils.trimAllWhitespace(response.body()));
    }

    @Test
    public void webSocketNotFoundTest() throws Exception {
        int port = WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/unknown-resource"))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("404 Not Found", response.body());
    }

    @Test
    public void webSocketPostMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.noBody()));
    }

    @Test
    public void webSocketPutMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.noBody()));
    }

    @Test
    public void webSocketDeleteMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().DELETE());
    }

    private void methodNotAllowedTest(HttpRequest.Builder requestBuilder) throws Exception {
        int port = WebSocketServerVertical.BOUND_PORTS.values().stream().findFirst().orElseThrow();

        HttpClient client = HttpClient.newHttpClient();
        requestBuilder.uri(URI.create("http://localhost:" + port));

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertEquals("403 Forbidden", response.body());
    }

    private static final String EXPECTED_HTML = """
            <html>
            <head><title>Web Socket Test</title></head>
            <body>
            <script type="text/javascript">
                var socket;
                if (!window.WebSocket) {
                    window.WebSocket = window.MozWebSocket;
                }
                if (window.WebSocket) {
                    socket = new WebSocket("ws://localhost:%d/websocket");
                    socket.onmessage = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = ta.value + '\\n' + event.data
                    };
                    socket.onopen = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = "Web Socket opened!";
                    };
                    socket.onclose = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = ta.value + "Web Socket closed";
                    };
                } else {
                    alert("Your browser does not support Web Socket.");
                }
                        
                function send(message) {
                    if (!window.WebSocket) {
                        return;
                    }
                    if (socket.readyState == WebSocket.OPEN) {
                        socket.send(message);
                    } else {
                        alert("The socket is not open.");
                    }
                }
            </script>
            <form onsubmit="return false;">
                <input type="text" name="message" value="Hello, World!"/><input type="button" value="Send Web Socket Data"
                                                                                onclick="send(this.form.message.value)"/>
                <h3>Output</h3>
                <textarea id="responseText" style="width:500px;height:300px;"></textarea>
            </form>
            </body>
            </html>""";


}