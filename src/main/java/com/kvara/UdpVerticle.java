package com.kvara;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class UdpVerticle extends AbstractVerticle {


    @Override
    public void start(io.vertx.core.Promise<Void> startPromise) throws Exception {

        System.out.println("Start UdpVerticle");
        ExecutorService executor = Executors.newFixedThreadPool(10);

        executor.submit(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new QuoteOfTheMomentServerHandler());

                System.out.println("binding UDP port");
                b.bind(8888).sync().channel().closeFuture().await();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                group.shutdownGracefully();
            }
        });

        System.out.println("completing UdpVerticle promise");

        startPromise.complete();
    }


}