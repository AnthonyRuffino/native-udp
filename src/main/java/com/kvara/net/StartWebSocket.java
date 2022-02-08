package com.kvara.net;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kvara.HelloRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/{address}")
@ApplicationScoped
public class StartWebSocket {

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("address") String address) {
        System.out.println("session: " + session.getId());
        sessions.put(address, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("address") String address) {
        sessions.remove(address);
        broadcast("User " + address + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("address") String address, Throwable throwable) {
        sessions.remove(address);
        broadcast("User " + address + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(Session session, ByteBuffer message, @PathParam("address") String address) throws InvalidProtocolBufferException {
        var x = HelloRequest.parseFrom(message);
        System.out.println("");
    }

    private void broadcast(String message) {
        sessions.values().forEach(s ->
                s.getAsyncRemote().sendObject(message, result -> {
                    if (result.getException() != null) {
                        System.out.println("Unable to send message: " + result.getException());
                    }
                })
        );
    }
}
