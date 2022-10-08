package com.kvara.udp;


import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component("udpMessageParser")
public class UdpMessageParser implements Function<ByteBuffer, Optional<UdpParsedMessage>> {

    private final Character deliminator;

    public UdpMessageParser(@Value("${com.kvara.udp.UdpMessageParser.deliminator:|}") Character deliminator) {
        this.deliminator = deliminator;
    }

    @Override
    public Optional<UdpParsedMessage> apply(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        return getAddress(content, capacity)
                .map(address ->
                        new UdpParsedMessage(address, getData(content, capacity))
                );
    }

    private byte[] getData(ByteBuffer content, int capacity) {
        List<Byte> remainingBytes = new ArrayList<>();
        while (content.position() < capacity) {
            remainingBytes.add(content.get());
        }
        return ArrayUtils.toPrimitive(remainingBytes.toArray(new Byte[0]));
    }

    private Optional<String> getAddress(ByteBuffer content, int capacity) {
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
