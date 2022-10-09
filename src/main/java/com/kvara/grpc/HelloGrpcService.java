package com.kvara.grpc;

import com.kvara.HelloGrpc;
import com.kvara.HelloReply;
import com.kvara.HelloRequest;
import com.kvara.HelloVerticle;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloGrpcService implements HelloGrpc {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom()
                .item("Hello " + request.getName() + "!")
                .map(msg ->
                        HelloReply.newBuilder()
                                .setMessage(msg)
                                .setAdvice(HelloVerticle.MESSAGE_TAKE_CARE_ADVICE)
                                .build()
                );
    }

}
