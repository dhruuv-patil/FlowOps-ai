package com.flowops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * FlowOps API — Spring Boot entry point.
 *
 * <p>The API server is the only service the browser talks to; it owns
 * authentication, multi-tenant authorization, the workflow model, the
 * execution engine, and brokers all calls to the Python AI service.
 *
 * <p>{@link ConfigurationPropertiesScan} registers
 * {@link com.flowops.config.AuthProperties} as a bean. Without it a
 * {@code @ConfigurationProperties} record is never bound, and every injection
 * point that needs it — {@code JwtService}, {@code AuthPropertiesValidator},
 * the BCrypt encoder — fails the context startup.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FlowOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowOpsApplication.class, args);
    }
}
