package com.kvara.io.udp;


import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class UdpVerticleDeployer {

    public void init(@Observes StartupEvent e, Vertx vertx, UdpServerVertical udpServerVertical) {
        vertx.deployVerticle(udpServerVertical).await().indefinitely();
    }
}
