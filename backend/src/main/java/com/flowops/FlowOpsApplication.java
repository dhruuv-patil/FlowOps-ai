package com.flowops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FlowOps API — Spring Boot entry point.
 *
 * <p>The API server is the only service the browser talks to; it owns
 * authentication, multi-tenant authorization, the workflow model, the
 * execution engine, and brokers all calls to the Python AI service.
 */
@SpringBootApplication
public class FlowOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowOpsApplication.class, args);
    }
}
