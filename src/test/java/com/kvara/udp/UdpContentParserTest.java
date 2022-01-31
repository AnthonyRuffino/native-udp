package com.kvara.udp;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UdpContentParserTest {

    private static final char DELIMINATOR = '|';
    private static final String ADDRESS = "eventBusAddress";
    private static final String DATA = "data";

    private static final UdpMessageParser ADDRESS_PARSER = new UdpMessageParser(DELIMINATOR);

    @Test
    void verifyAddressAndContent() {
        ByteBuffer bufferedContent = ByteBuffer.wrap((ADDRESS + DELIMINATOR + DATA).getBytes(StandardCharsets.UTF_8));
        ADDRESS_PARSER.apply(bufferedContent)
                .ifPresentOrElse(
                        udpParsedMessage -> {
                            assertEquals(ADDRESS, udpParsedMessage.address());
                            assertEquals(DATA, new String(udpParsedMessage.data()));
                        },
                        () -> fail("udpParsedMessage was empty")
                );
    }
}