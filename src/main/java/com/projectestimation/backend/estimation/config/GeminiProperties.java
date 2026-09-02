package com.projectestimation.backend.estimation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {


    private static String apiKey = "AQ.Ab8RN6LR5lgdWlpld9fF9zT3HOzaBvB1sdRAC8kzw_Zb8s4faA"; //put your genini key here and never push it to git

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
