package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.cache.ErrorCacheService;
import org.example.client.ApiClient;

public class NettyServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        ApiClient apiClient = new ApiClient();
        ErrorCacheService redis = new ErrorCacheService();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer(null,apiClient, redis))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel = bootstrap.bind(PORT).sync().channel();

            System.out.println("Server started on http://localhost:" + PORT);

            channel.closeFuture().sync();
        } finally {
            System.out.println("Shutting down server...");

            redis.shutdown();

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
