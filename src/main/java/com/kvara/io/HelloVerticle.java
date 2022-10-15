package com.kvara.io;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kvara.HelloReply;
import com.kvara.HelloRequest;
import io.quarkus.vertx.ConsumeEvent;
import io.worldy.sockiopath.messaging.SockiopathMessage;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloVerticle {

    public static final String MESSAGE_TAKE_CARE_ADVICE = "Take care!";
    public static final String MESSAGE_VULCAN_ADVICE = "Live long and prosper!";

    @ConsumeEvent("hello")
    public byte[] hello(SockiopathMessage message) throws InvalidProtocolBufferException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message.data());


        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("Hello " + helloRequest.getName())
                .setAdvice(MESSAGE_TAKE_CARE_ADVICE)
                .build();

        return helloReply.toByteArray();
    }

    @ConsumeEvent("goodbye")
    public byte[] goodbye(SockiopathMessage message) throws InvalidProtocolBufferException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message.data());

        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("Goodbye " + helloRequest.getName())
                .setAdvice(MESSAGE_VULCAN_ADVICE)
                .build();

        return helloReply.toByteArray();
    }

    @ConsumeEvent("callback")
    public byte[] callback(SockiopathMessage message) throws InvalidProtocolBufferException, InterruptedException {
        HelloRequest helloRequest = HelloRequest.parseFrom(message.data());

        HelloReply helloReply = HelloReply.newBuilder()
                .setMessage("I'll call you back " + helloRequest.getName())
                .setAdvice("Be patient!")
                .build();

        return helloReply.toByteArray();
    }
}
