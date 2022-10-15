package com.kvara.io;

import com.kvara.io.udp.CallbackThread;
import io.netty.channel.ChannelHandlerContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.mutiny.core.eventbus.Message;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.session.SockiopathSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class SockiopathServerVertical extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(SockiopathServerVertical.class);

    protected final String id;

    int actualPort;

    public SockiopathServerVertical() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("starting SockiopathServerVertical: " + this.getClass().getTypeName());

        try {
            CompletableFuture<StartServerResult> startFuture = sockiopathServer().start().orTimeout(getStartTimeoutMillis(), TimeUnit.MILLISECONDS);
            actualPort = startFuture.get().port();
            startPromise.complete();
        } catch (InterruptedException | ExecutionException e) {
            startPromise.fail(e.getCause());
        }
    }

    public Integer actualPort() {
        return actualPort;
    }

    protected abstract SockiopathServer sockiopathServer();

    protected int getStartTimeoutMillis() {
        return 1000;
    }

    protected LocalMap<String, SockiopathSession> getSessions() {
        return vertx.getDelegate().sharedData().getLocalMap("sessions");
    }

    protected Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "hello", new MessageBus(getEventBusBridge(null), 1000),
                "goodbye", new MessageBus(getEventBusBridge(null), 1000),
                "callback", new MessageBus(getEventBusBridge((ctx, sender) -> {
                    CallbackThread t = new CallbackThread(ctx, sender, 100);
                    t.start();
                }), 1000)
        );
    }

    protected Function<SockiopathMessage, CompletableFuture<byte[]>> getEventBusBridge(BiConsumer<ChannelHandlerContext, InetSocketAddress> hook) {
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
