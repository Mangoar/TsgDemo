package org.example.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import org.example.cache.ErrorCacheService;
import org.example.client.ApiClient;
import org.example.exception.ApiDashboardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class DashboardApiHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApiClient apiClient;
    private final ErrorCacheService redis;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(DashboardApiHandler.class);

    public DashboardApiHandler(ApiClient apiClient, ErrorCacheService redis) {
        this.apiClient = apiClient;
        this.redis = redis;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

        log.info("Incoming request: {} {}", req.method(), req.uri());

        String requestString = req.method()+" "+req.uri();
        if (req.method() != HttpMethod.GET) {
            handleError(new ApiDashboardException(HttpResponseStatus.METHOD_NOT_ALLOWED,
                    "Only GET is supported"), requestString);
            sendError(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        } else if (!"/api/dashboard".equals(req.uri())){
            handleError(new ApiDashboardException(HttpResponseStatus.NOT_FOUND,
                    "Endpoint not found"),requestString);
            sendError(ctx,HttpResponseStatus.NOT_FOUND);
            return;
        }

        handleDashboard(ctx);
    }

    private void handleError(Throwable ex, String requestStr) {

        log.error("Dashboard failed", ex);

        try {
            ObjectNode error = mapper.createObjectNode();
            error.put("timestamp", Instant.now().toString());
            error.put("endpoint", requestStr);
            error.put("error", ex.getMessage());

            String json = mapper.writeValueAsString(error);

            String redisKey = "dashboard:error:" + System.currentTimeMillis();

            redis.saveError(redisKey, json)
                    .exceptionally(e -> {
                        log.error("Redis write failed", e);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to write error JSON", e);
        }
    }

    private void handleDashboard(ChannelHandlerContext ctx) {

        log.info("Dashboard request started");

        CompletableFuture<String> weather = apiClient.getWeather().whenComplete((r,e)->log.info("Weather fetched"));
        CompletableFuture<String> fact = apiClient.getFact().whenComplete((r, e) -> log.info("Fact completed"));
        CompletableFuture<String> ip = apiClient.getIp().whenComplete((r, e) -> log.info("IP completed"));
        CompletableFuture
                .allOf(weather, fact, ip)
                .thenApply(v -> buildJson(
                        weather.join(),
                        fact.join(),
                        ip.join()
                ))
                .thenAccept(json -> {
                    sendJson(ctx, json);
                })
                .whenComplete((r, e) -> log.info("Dashboard response sent"))
                .exceptionally(ex -> {
                    handleError(ex,"GET api/dashboard");
                    return null;
                });
    }

    private String buildJson(String weather, String fact, String ip) {

        try {
            ObjectNode root = mapper.createObjectNode();

            ObjectNode data = root.putObject("data");
            data.set("weather", mapper.readTree(weather));
            data.set("fact", mapper.readTree(fact));
            data.set("ip", mapper.readTree(ip));

            ObjectNode meta = root.putObject("meta");
            meta.put("timestamp", Instant.now().toString());
            meta.put("source", "live");

            return mapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendJson(ChannelHandlerContext ctx, String json) {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(bytes)
                );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        status
                );

        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
