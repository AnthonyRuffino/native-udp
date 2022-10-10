package com.kvara.websocket;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class WebSocketVerticleDeployer {
    public void init(@Observes StartupEvent startupEvent, Vertx vertx, WebSocketServerVertical websocketServerVertical) {
        vertx.deployVerticle(websocketServerVertical).await().indefinitely();
    }
}
