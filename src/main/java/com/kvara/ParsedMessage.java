package com.kvara;

public record ParsedMessage(
        String address,
        byte[] data
) {
}
