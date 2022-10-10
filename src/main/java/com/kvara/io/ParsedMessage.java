package com.kvara.io;

public record ParsedMessage(
        String address,

        String sessionId,
        byte[] data
) {
}
