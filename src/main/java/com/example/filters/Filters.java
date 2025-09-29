package com.example.filters;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "security.api-key", defaultValue = "")
    String apiKey;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if (apiKey == null || apiKey.isBlank()) return; // disabled
        String provided = ctx.getHeaderString("X-API-Key");
        if (provided == null || !provided.equals(apiKey)) {
            ctx.abortWith(jakarta.ws.rs.core.Response
                    .status(401)
                    .entity(Map.of("error","unauthorized","message","Missing or invalid API key"))
                    .build());
        }
    }
}

@Provider
@Priority(Priorities.USER)
class RateLimitFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "rate.limit.capacity", defaultValue = "60")
    int capacity;
    @ConfigProperty(name = "rate.limit.refillPerMinute", defaultValue = "60")
    int refillPerMinute;

    static class Bucket {
        AtomicInteger tokens;
        volatile long lastRefillEpochMinute;
        Bucket(int capacity, long minute) { this.tokens = new AtomicInteger(capacity); this.lastRefillEpochMinute = minute; }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String key = ctx.getHeaderString("X-Forwarded-For");
        if (key == null) key = ctx.getHeaders().getFirst("X-Real-IP");
        if (key == null) key = ctx.getUriInfo().getRequestUri().getHost(); // fallback
        long minute = Instant.now().getEpochSecond() / 60;
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(capacity, minute));
        synchronized (b) {
            if (b.lastRefillEpochMinute != minute) {
                b.tokens.set(capacity);
                b.lastRefillEpochMinute = minute;
            }
            if (b.tokens.get() <= 0) {
                ctx.abortWith(jakarta.ws.rs.core.Response
                        .status(429)
                        .entity(Map.of("error","rate_limited","message","Too many requests"))
                        .build());
                return;
            }
            b.tokens.decrementAndGet();
        }
    }
}

@Provider
@Priority(Priorities.HEADER_DECORATOR)
class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String query = requestContext.getUriInfo().getRequestUri().getQuery();
        System.out.printf("Incoming %s %s?%s%n", requestContext.getMethod(), path, query);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        System.out.printf("Outgoing %d for %s%n", responseContext.getStatus(), requestContext.getUriInfo().getPath());
    }
}
