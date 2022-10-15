package com.kvara.io.udp;

import com.kvara.io.SharedSockiopathSession;
import com.kvara.io.SockiopathServerVertical;
import io.netty.channel.ChannelHandlerContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.mutiny.core.eventbus.Message;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.UdpHandler;
import io.worldy.sockiopath.udp.UdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.enterprise.context.ApplicationScoped;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
                //new UdpMessageHandler(vertx, messageParser),
                channelHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }

    private LocalMap<String, SockiopathSession> getSessions() {
        return vertx.getDelegate().sharedData().getLocalMap("sessions");
    }

    private Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "hello", new MessageBus(getEventBusBridge(null), 1000),
                "goodbye", new MessageBus(getEventBusBridge(null), 1000),
                "callback", new MessageBus(getEventBusBridge((ctx, sender) -> {
                    CallbackThread t = new CallbackThread(ctx, sender, 100);
                    t.start();
                }), 1000)
        );
    }

    private Function<SockiopathMessage, CompletableFuture<byte[]>> getEventBusBridge(BiConsumer<ChannelHandlerContext, InetSocketAddress> hook) {
        return (parsedMessage) -> {
            CompletableFuture<byte[]> future = new CompletableFuture<>();

            Uni<Message<byte[]>> request = vertx.eventBus().request(parsedMessage.address(), parsedMessage);
            request.onItem()
                    .transform(b -> future.complete(b.body()))
                    .subscribe()
                    .with((completionResponse) -> {
                        logger.debug("Message was successful: " + completionResponse);
                        if (hook != null) {
                            SockiopathSession session = getSessions().get(parsedMessage.sessionId());
                            hook.accept(session.getUdpContext(), session.getUdpSocketAddress());
                        }
                    });

            return future;
        };
    }
}
