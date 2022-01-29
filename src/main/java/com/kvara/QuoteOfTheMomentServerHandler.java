package com.kvara;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.util.Random;

public class QuoteOfTheMomentServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Random random = new Random();

    // Quotes from Mohandas K. Gandhi:
    private static final String[] quotes = {
            "Where there is love there is life.",
            "First they ignore you, then they laugh at you, then they fight you, then you win.",
            "Be the change you want to see in the world.",
            "The weak can never forgive. Forgiveness is the attribute of the strong.",
    };

    private String nextQuote() {
        int quoteId;
        synchronized (random) {
            quoteId = random.nextInt(quotes.length);
        }
        return quotes[quoteId];
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        System.err.println(packet);

        String message = packet.content().toString(CharsetUtil.UTF_8).replace(System.getProperty("line.separator"), "");
        System.out.println("Message: " + message + ".");
        if ("QOTM?".equals(message)) {
            ctx.write(new DatagramPacket(
                    Unpooled.copiedBuffer("QOTM: " + nextQuote(), CharsetUtil.UTF_8), packet.sender()));
        } else {
            System.out.println("Message");
            ctx.write(new DatagramPacket(
                    Unpooled.copiedBuffer("What do you want? " + message, CharsetUtil.UTF_8), packet.sender()));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        // We don't close the channel because we can keep serving requests.
    }
}
