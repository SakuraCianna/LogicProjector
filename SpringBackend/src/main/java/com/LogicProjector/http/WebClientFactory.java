package com.LogicProjector.http;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebClientFactory {

    private final WebClient.Builder builder;

    public WebClientFactory(WebClient.Builder builder) {
        this.builder = builder;
    }

    public WebClient forBaseUrl(String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .build();
    }
}
