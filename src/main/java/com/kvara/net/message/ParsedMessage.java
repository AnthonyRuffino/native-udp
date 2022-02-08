package com.kvara.net.message;

public record ParsedMessage(
        String sessionId,
        String address,
        byte[] data
) {
}
