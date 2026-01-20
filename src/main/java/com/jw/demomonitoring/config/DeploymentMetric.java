package com.jw.demomonitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DeploymentMetric {

    private final AtomicInteger deploying;

    private static final String DEPLOYMENT_IN_PROGRESS = "deployment_in_progress";

    public DeploymentMetric(MeterRegistry registry) {
        this.deploying = registry.gauge(
                DEPLOYMENT_IN_PROGRESS,
                new AtomicInteger(0)
        );
    }

    public void startDeploy() {
        deploying.set(1);
    }

    public void endDeploy() {
        deploying.set(0);
    }
}
