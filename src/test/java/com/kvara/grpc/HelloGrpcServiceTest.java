package com.kvara.grpc;

import com.kvara.HelloGrpc;
import com.kvara.HelloReply;
import com.kvara.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class HelloGrpcServiceTest {

    @GrpcClient
    HelloGrpc helloGrpc;

    @Test
    public void testHello() {
        HelloReply reply = helloGrpc
                .sayHello(
                        HelloRequest.newBuilder().setName("Neo").build()
                )
                .await()
                .atMost(Duration.ofSeconds(5));
        assertEquals("Hello Neo!", reply.getMessage());
        assertEquals("Take care!", reply.getAdvice());
    }

}
