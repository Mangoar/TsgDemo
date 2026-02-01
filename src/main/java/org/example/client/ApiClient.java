package org.example.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ApiClient {

    private final HttpClient client;

    private static final URI WEATHER_URI = URI.create("https://api.open-meteo.com/v1/forecast?latitude=51.107883&longitude=17.038538&current_weather=true");
    private static final URI FACT_URI = URI.create("https://uselessfacts.jsph.pl/api/v2/facts/random");
    private static final URI IP_URI = URI.create("https://api.ipify.org/?format=json");
    private static final String FALLBACK_WEATHER_JSON =
            """
            {
              "error": "weather_unavailable",
              "source": "fallback"
            }
            """;
    private static final String FALLBACK_FACT_JSON =
            """
            {
              "error": "fact_unavailable",
              "source": "fallback"
            }
            """;
    private static final String FALLBACK_IP_JSON =
            """
            {
              "error": "ip_unavailable",
              "source": "fallback"
            }
            """;

    public ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public CompletableFuture<String> getWeather() {
        return withRetry(() -> send(WEATHER_URI), 2)
                .exceptionally(ex -> FALLBACK_WEATHER_JSON);
    }

    public CompletableFuture<String> getFact() {
        return withRetry(() -> send(FACT_URI), 2)
                .exceptionally(ex -> FALLBACK_FACT_JSON);
    }

    public CompletableFuture<String> getIp() {
        return withRetry(() -> send(IP_URI), 2)
                .exceptionally(ex -> FALLBACK_IP_JSON);
    }

    private CompletableFuture<String> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    private CompletableFuture<String> withRetry(
            Supplier<CompletableFuture<String>> call,
            int retries
    ) {
        return call.get().handle((result, ex) -> {
            if (ex == null) {
                return CompletableFuture.completedFuture(result);
            }
            if (retries > 0) {
                return withRetry(call, retries - 1);
            }
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }).thenCompose(f -> f);
    }

}
