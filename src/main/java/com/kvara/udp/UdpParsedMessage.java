package com.kvara.udp;

public record UdpParsedMessage(
        String address,
        byte[] data
) {
}
