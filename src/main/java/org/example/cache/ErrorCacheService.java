package org.example.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.concurrent.CompletableFuture;

public class ErrorCacheService {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisAsyncCommands<String, String> redis;

    public ErrorCacheService() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String port = System.getenv().getOrDefault("REDIS_PORT", "6379");
        this.client = RedisClient.create("redis://" + host + ":" + port);
        this.connection = client.connect();
        this.redis = connection.async();
    }

    public CompletableFuture<Void> saveError(String key, String json) {
        return redis.set(key, json).toCompletableFuture().thenApply(r -> null);
    }

    public void shutdown() {
        connection.close();
        client.shutdown();
    }

}
