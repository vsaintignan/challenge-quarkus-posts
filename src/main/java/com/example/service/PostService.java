package com.example.service;

import com.example.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.Retry;
import io.smallrye.faulttolerance.api.Timeout;
import io.smallrye.common.annotation.NonBlocking;
import io.quarkus.cache.CacheResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostService {

    @ConfigProperty(name = "jsonplaceholder.base-url")
    String baseUrl;

    private final ObjectMapper mapper;
    private final HttpClient client;
    private final ExecutorService pool = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));

    public PostService() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(pool)
                .build();
    }

    public List<MergedPost> getMergedPosts(Integer limit, Boolean includeComments, Integer userIdFilter) {
        try {
            List<Post> posts = fetchPosts();
            if (userIdFilter != null) {
                posts = posts.stream().filter(p -> p.userId == userIdFilter).collect(Collectors.toList());
            }
            if (limit != null && limit > 0) {
                posts = posts.stream().limit(limit).collect(Collectors.toList());
            }

            // For each post, fetch user & comments concurrently
            Map<Integer, CompletableFuture<User>> userCache = new HashMap<>();
            List<CompletableFuture<MergedPost>> futures = new ArrayList<>();

            for (Post p : posts) {
                CompletableFuture<User> userFuture = userCache.computeIfAbsent(p.userId, uid -> fetchUserAsync(uid));
                CompletableFuture<List<Comment>> commentsFuture = (includeComments != null && includeComments)
                        ? fetchCommentsAsync(p.id)
                        : CompletableFuture.completedFuture(Collections.emptyList());
                CompletableFuture<MergedPost> merged = userFuture.thenCombine(commentsFuture, (u, comments) -> new MergedPost(p, u, comments));
                futures.add(merged);
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            all.join();
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener/combinar posts: " + e.getMessage(), e);
        }
    }

    
@Retry(maxRetries = 2, delay = 200)
@Timeout(5000)
@CircuitBreakerName("posts")
private List<Post> fetchPosts() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/posts"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("API externa /posts devolvió " + resp.statusCode());
        }
        return mapper.readValue(resp.body(), new TypeReference<List<Post>>(){});
    }

    
@Retry(maxRetries = 2, delay = 200)
@Timeout(5000)
@CircuitBreakerName("comments")
private CompletableFuture<List<Comment>> fetchCommentsAsync(int postId) {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/posts/" + postId + "/comments"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() >= 400) {
                        throw new RuntimeException("API externa /posts/" + postId + "/comments devolvió " + resp.statusCode());
                    }
                    try {
                        return mapper.readValue(resp.body(), new TypeReference<List<Comment>>(){});
                    } catch (Exception e) {
                        throw new RuntimeException("Error parseando comentarios: " + e.getMessage(), e);
                    }
                });
    }

    
@CacheResult(cacheName = "users")
@Retry(maxRetries = 2, delay = 200)
@Timeout(5000)
@CircuitBreakerName("users")
private CompletableFuture<User> fetchUserAsync(int userId) {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + userId))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() >= 400) {
                        throw new RuntimeException("API externa /users/" + userId + " devolvió " + resp.statusCode());
                    }
                    try {
                        return new ObjectMapper()
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                .readValue(resp.body(), User.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Error parseando usuario: " + e.getMessage(), e);
                    }
                });
    }
}
