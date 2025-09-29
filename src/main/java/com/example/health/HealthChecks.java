package com.example.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("live");
    }
}

@Readiness
@ApplicationScoped
class ReadinessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        // Could add an outbound ping if needed
        return HealthCheckResponse.up("ready");
    }
}
