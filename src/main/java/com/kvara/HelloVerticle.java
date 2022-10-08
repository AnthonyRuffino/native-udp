package com.kvara;

import com.google.protobuf.InvalidProtocolBufferException;
import io.quarkus.vertx.ConsumeEvent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloVerticle {

    static final String MESSAGE_TAKE_CARE_ADVICE = "Take care!";
    static final String MESSAGE_VULCAN_ADVICE = "Live long and prosper!";

    @ConsumeEvent("hello")
    public byte[] hello(byte[] message) throws InvalidProtocolBufferException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message);


        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("Hello " + helloRequest.getName())
                .setAdvice(MESSAGE_TAKE_CARE_ADVICE)
                .build();

        return helloReply.toByteArray();
    }

    @ConsumeEvent("goodbye")
    public byte[] goodbye(byte[] message) throws InvalidProtocolBufferException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message);

        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("Goodbye " + helloRequest.getName())
                .setAdvice(MESSAGE_VULCAN_ADVICE)
                .build();

        return helloReply.toByteArray();
    }

}