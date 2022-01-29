package com.kvara;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class VerticleDeployer {

    public void init(@Observes StartupEvent e, Vertx vertx, UdpVerticle verticle) {
        vertx.deployVerticle(verticle).await().indefinitely();
    }
}
