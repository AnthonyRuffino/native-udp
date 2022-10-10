package com.kvara.test.udp;

import com.kvara.test.AbstractTestClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UdpBootstrappedTestClient extends AbstractTestClient {

    /**
     * Constructor
     *
     * @param port {@link Integer} port of server
     */
    public UdpBootstrappedTestClient(String host, int port, int connectTimeout) {
        super(host, port, connectTimeout);
    }


    /**
     * Startup the client
     *
     * @return {@link ChannelFuture}
     * @throws Exception
     */
    public void startup() {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new ChannelInitializer<DatagramChannel>() {
                protected void initChannel(DatagramChannel datagramChannel) {
                    datagramChannel.pipeline().addLast(
                            new SimpleChannelInboundHandler<DatagramPacket>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {

                                    debugMessage(byteBufferToString(msg.copy().content().nioBuffer()));
                                    assertions.get(latch.getCount()).setResponse(msg.copy());
                                    latch.countDown();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelActive(ctx);
                                }
                            }
                    );
                }
            });
            ChannelFuture channelFuture = b.connect(host, this.port);
            if (!channelFuture.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Client took too long to connect");
            }
            this.channel = channelFuture.channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String byteBufferToString(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        return new String(getData(content, capacity));
    }

    private byte[] getData(ByteBuffer content, int capacity) {
        List<Byte> remainingBytes = new ArrayList<>();
        while (content.position() < capacity) {
            remainingBytes.add(content.get());
        }
        return ArrayUtils.toPrimitive(remainingBytes.toArray(new Byte[0]));
    }

}
