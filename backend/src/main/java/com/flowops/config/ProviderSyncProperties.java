package com.flowops.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for provider sync behavior.
 */
@Component
@ConfigurationProperties(prefix = "flowops.providers")
public class ProviderSyncProperties {

    /**
     * Delay between sync sweeps. Default 1 minute.
     */
    private Duration syncDelay = Duration.ofMinutes(1);

    /**
     * Page size for fetching executions from providers. Default 50.
     */
    private int pageSize = 50;

    /**
     * Connect timeout for provider HTTP calls. Default 10 seconds.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Request timeout for provider HTTP calls. Default 30 seconds.
     */
    private Duration requestTimeout = Duration.ofSeconds(30);

    public Duration getSyncDelay() {
        return syncDelay;
    }

    public void setSyncDelay(Duration syncDelay) {
        this.syncDelay = syncDelay;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}