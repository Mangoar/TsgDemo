package org.example.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;
import org.example.cache.ErrorCacheService;
import org.example.client.ApiClient;
import org.example.handler.DashboardApiHandler;

public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final ApiClient apiClient;
    private final ErrorCacheService redis;

    public NettyServerInitializer(SslContext sslCtx, ApiClient apiClient, ErrorCacheService redis) {
        this.sslCtx = sslCtx;
        this.apiClient = apiClient;
        this.redis = redis;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(65536));
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new DashboardApiHandler(apiClient,redis));
    }
}
