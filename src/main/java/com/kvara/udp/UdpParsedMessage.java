package com.kvara.udp;

import java.util.Optional;

public record UdpParsedMessage(
        String address,
        byte[] data
) {
}
