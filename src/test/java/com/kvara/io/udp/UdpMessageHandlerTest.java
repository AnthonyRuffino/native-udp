package com.kvara.io.udp;

import io.netty.channel.ChannelHandlerContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.shareddata.SharedData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

@QuarkusTest
@TestProfile(UdpMessageHandlerTest.BuildTimeValueChangeTestProfile.class)
class UdpMessageHandlerTest {

    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.category.\"com.kvara.io.udp.UdpMessageHandler\".level", "OFF"
            );
        }
    }

    @Test
    void exceptionCaught() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Vertx vertx = Mockito.mock(Vertx.class);
        SharedData sharedData = Mockito.mock(SharedData.class);
        Mockito.when(vertx.sharedData()).thenReturn(sharedData);

        UdpMessageHandler udpMessageHandler = new UdpMessageHandler(vertx, null);
        udpMessageHandler.exceptionCaught(ctx, new RuntimeException("Mocked exception"));

        Mockito.verify(ctx, Mockito.never()).close();
    }
}
