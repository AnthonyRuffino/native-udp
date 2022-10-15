package com.kvara.io.udp;

import com.kvara.io.SharedSockiopathSession;
import com.kvara.io.SockiopathServerVertical;
import io.vertx.core.shareddata.LocalMap;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.UdpHandler;
import io.worldy.sockiopath.udp.UdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.Executors;

@ApplicationScoped
public class UdpServerVertical extends SockiopathServerVertical {

    private static final Logger logger = LoggerFactory.getLogger(UdpServerVertical.class);

    @Value("${com.kvara.io.udp.server.port}")
    int port;

    @Value("${com.kvara.io.message.deliminator:|}")
    Character deliminator;

    public UdpServerVertical() {
        super();
    }

    @Override
    protected SockiopathServer sockiopathServer() {
        LocalMap<String, SockiopathSession> sessionMap = getSessions();
        SessionStore<SockiopathSession> sessionStore = SharedSockiopathSession.localMapMapBackedSessionStore(sessionMap);
        UdpHandler channelHandler = new UdpHandler(sessionStore, getMessageHandlers(), deliminator);
        return new UdpServer(
                channelHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }
}
