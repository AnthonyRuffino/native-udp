package com.kvara.io.udp;

import io.netty.channel.ChannelHandlerContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.Map;

@QuarkusTest
@TestProfile(CallbackThreadTest.BuildTimeValueChangeTestProfile.class)
class CallbackThreadTest {

    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.category.\"com.kvara.io.udp.CallbackThread\".level", "OFF"
            );
        }
    }

    @Test
    void failedRun() {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        new CallbackThread(ctx, sender, 100).sendCallback();
        Mockito.verify(ctx, Mockito.never()).close();
    }
}
