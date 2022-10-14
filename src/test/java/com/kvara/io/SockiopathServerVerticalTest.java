package com.kvara.io;

import io.vertx.core.Promise;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SockiopathServerVerticalTest {

    Promise<Void> promise = Promise.promise();

    @Test
    void startFutureTimesOut() {
        new SockiopathServerVertical() {
            @Override
            protected SockiopathServer sockiopathServer() {
                return new SockiopathServer() {
                    @Override
                    public CompletableFuture<StartServerResult> start() {
                        return new CompletableFuture<>();
                    }

                    @Override
                    public int actualPort() {
                        return 0;
                    }
                };
            }
        }.start(promise);

        assertTrue(promise.future().failed(), "The promise given to SockiopathServerVertical.start should have failed.");
        assertThat(promise.future().cause(), instanceOf(TimeoutException.class));
    }

    @Test
    void startFutureFailsToExecute() {
        new SockiopathServerVertical() {
            @Override
            protected SockiopathServer sockiopathServer() {
                return new SockiopathServer() {
                    @Override
                    public CompletableFuture<StartServerResult> start() {
                        CompletableFuture<StartServerResult> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(
                                new ExecutionException(
                                        "Test ExecutionException",
                                        new RuntimeException("Mock reason.")
                                )
                        );
                        return failedFuture;
                    }

                    @Override
                    public int actualPort() {
                        return 0;
                    }
                };
            }
        }.start(promise);

        assertTrue(promise.future().failed(), "The promise given to SockiopathServerVertical.start should have failed.");
        assertThat(promise.future().cause(), instanceOf(ExecutionException.class));
    }
}
