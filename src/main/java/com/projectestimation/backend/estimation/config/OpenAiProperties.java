package com.projectestimation.backend.estimation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer connectTimeoutMs = 30000;
    private Integer readTimeoutMs = 180000;

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

	public Integer getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(Integer connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public Integer getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public void setReadTimeoutMs(Integer readTimeoutMs) {
		this.readTimeoutMs = readTimeoutMs;
	}
    
    
}
