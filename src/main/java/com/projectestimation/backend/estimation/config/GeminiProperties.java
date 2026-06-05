package com.projectestimation.backend.estimation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    private String apiKey = "AQ.Ab8RN6I9xVn-RRVBYsyjmPipsgzDsU2tAKTBhuuPXjlp0Zne0w";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "gemini-3.1-flash-lite";
    private int timeoutMs = 60_000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
