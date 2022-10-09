package com.kvara;

import com.kvara.udp.UdpServerVertical;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class VerticleDeployer {

    public void init(@Observes StartupEvent e, Vertx vertx, UdpServerVertical udpServerVertical) {
        vertx.deployVerticle(udpServerVertical).await().indefinitely();
    }
}
