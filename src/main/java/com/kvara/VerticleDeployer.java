package com.kvara;

import com.kvara.udp.UdpServer;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class VerticleDeployer {

    public void init(@Observes StartupEvent e, Vertx vertx, UdpServer udpServer) {
        vertx.deployVerticle(udpServer).await().indefinitely();
    }
}
