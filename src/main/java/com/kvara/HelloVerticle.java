package com.kvara;

import com.google.protobuf.InvalidProtocolBufferException;
import io.quarkus.vertx.ConsumeEvent;

import javax.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;

@ApplicationScoped
public class HelloVerticle {


    @ConsumeEvent("hello")
    public byte[] hello(byte[] message) throws InvalidProtocolBufferException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message);

        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("Hello " + helloRequest.getName())
                .setAdvice("care")
                .build();

        return helloReply.toByteArray();
    }

}