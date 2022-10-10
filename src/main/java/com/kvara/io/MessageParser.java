package com.kvara.io;


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

    public MessageParser(@Value("${com.kvara.io.message.deliminator:|}") Character deliminator) {
        this.deliminator = deliminator;
    }

    @Override
    public Optional<ParsedMessage> apply(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        return getPart(content, capacity)
                .flatMap(address ->
                        getPart(content, capacity).map(sessionId ->
                                new ParsedMessage(address, sessionId, getData(content, capacity))
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

    private Optional<String> getPart(ByteBuffer content, int capacity) {
        StringBuilder addressBuilder = new StringBuilder();
        boolean delimiterFound = false;
        while (content.position() < capacity) {
            byte singleByte = content.get();
            char character = (char) singleByte;
            if (deliminator.equals(character)) {
                delimiterFound = true;
                break;
            }
            addressBuilder.append(character);
        }
        return delimiterFound ? Optional.of(addressBuilder.toString()) : Optional.empty();
    }

}
