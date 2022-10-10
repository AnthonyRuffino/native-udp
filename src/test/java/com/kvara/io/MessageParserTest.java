package com.kvara.io;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MessageParserTest {

    private static final char DELIMINATOR = '|';
    private static final String ADDRESS = "eventBusAddress";

    private static final String SESSION_ID = "sessionId";
    private static final String DATA = "data";

    @Test
    void verifyAddressAndContent() {
        ByteBuffer bufferedContent = ByteBuffer.wrap((ADDRESS + DELIMINATOR + SESSION_ID + DELIMINATOR + DATA).getBytes(StandardCharsets.UTF_8));
        new MessageParser(DELIMINATOR).apply(bufferedContent)
                .ifPresentOrElse(
                        udpParsedMessage -> {
                            assertEquals(ADDRESS, udpParsedMessage.address());
                            assertEquals(SESSION_ID, udpParsedMessage.sessionId());
                            assertEquals(DATA, new String(udpParsedMessage.data()));
                        },
                        () -> fail("udpParsedMessage was empty")
                );
    }
}