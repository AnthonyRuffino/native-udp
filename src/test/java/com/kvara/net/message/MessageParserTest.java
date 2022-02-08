package com.kvara.net.message;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MessageParserTest {

    private static final char DELIMINATOR = '|';
    private static final String SESSION_ID = "sessionId";
    private static final String ADDRESS = "eventBusAddress";
    private static final String DATA = "data";

    private static final MessageParser MESSAGE_PARSER = new MessageParser(DELIMINATOR);

    @Test
    void verifyAddressAndContent() {
        ByteBuffer bufferedContent = ByteBuffer.wrap((SESSION_ID + DELIMINATOR + ADDRESS + DELIMINATOR + DATA).getBytes(StandardCharsets.UTF_8));
        MESSAGE_PARSER.apply(bufferedContent)
                .ifPresentOrElse(
                        udpParsedMessage -> {
                            assertEquals(SESSION_ID, udpParsedMessage.sessionId());
                            assertEquals(ADDRESS, udpParsedMessage.address());
                            assertEquals(DATA, new String(udpParsedMessage.data()));
                        },
                        () -> fail("udpParsedMessage was empty")
                );
    }
}