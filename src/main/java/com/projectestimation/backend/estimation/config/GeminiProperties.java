package com.projectestimation.backend.estimation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    private String apiKey = "AIzaSyAXPADxsq0IhHQHM0r9BdF5nUtjnWrjOvc";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "gemini-2.0-flash";
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
