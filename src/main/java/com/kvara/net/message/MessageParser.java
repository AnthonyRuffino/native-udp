package com.kvara.net.message;


import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component("messageParser")
public class MessageParser implements Function<ByteBuffer, Optional<ParsedMessage>> {

    private final Character deliminator;

    public MessageParser(@Value("${com.kvara.udp.UdpMessageParser.deliminator:|}") Character deliminator) {
        this.deliminator = deliminator;
    }

    @Override
    public Optional<ParsedMessage> apply(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        return parseMetadata(content, capacity)
                .map(metadata ->
                        new ParsedMessage(
                                metadata.sessionId,
                                metadata.address,
                                getData(content, capacity)
                        )
                );
    }

    private byte[] getData(ByteBuffer content, int capacity) {
        List<Byte> remainingBytes = new ArrayList<>();
        while (content.position() < capacity) {
            remainingBytes.add(content.get());
        }
        return ArrayUtils.toPrimitive(remainingBytes.toArray(new Byte[0]));
    }

    private Optional<Metadata> parseMetadata(ByteBuffer content, int capacity) {
        StringBuilder addressBuilder = new StringBuilder();
        StringBuilder sessionIdBuilder = new StringBuilder();
        boolean sessionIdFound = false;
        boolean addressFound = false;

        while (content.position() < capacity) {
            byte singleByte = content.get();
            char character = (char) singleByte;
            if (deliminator.equals(character)) {
                if (sessionIdFound) {
                    addressFound = true;
                    break;
                } else {
                    sessionIdFound = true;
                }
            } else {
                if (sessionIdFound) {
                    addressBuilder.append(character);
                } else {
                    sessionIdBuilder.append(character);
                }
            }
        }

        return addressFound ? buildMetadata(addressBuilder, sessionIdBuilder) : Optional.empty();
    }

    private Optional<Metadata> buildMetadata(StringBuilder addressBuilder, StringBuilder sessionIdBuilder) {
        return Optional.of(new Metadata(sessionIdBuilder.toString(), addressBuilder.toString()));
    }

    private static record Metadata(String sessionId, String address) {
    }

}
