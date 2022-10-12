package com.kvara.io;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
}
