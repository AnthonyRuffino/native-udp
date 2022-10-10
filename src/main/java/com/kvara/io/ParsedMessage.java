package com.kvara.io;

public record ParsedMessage(
        String address,
        byte[] data
) {
}
