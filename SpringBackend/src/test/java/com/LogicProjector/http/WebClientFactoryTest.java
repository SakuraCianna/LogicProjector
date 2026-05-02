package com.LogicProjector.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientFactoryTest {

    @Test
    void shouldCreateClientFromClonedBuilderWithBaseUrl() {
        WebClient.Builder sharedBuilder = mock(WebClient.Builder.class);
        WebClient.Builder requestBuilder = mock(WebClient.Builder.class);
        WebClient webClient = mock(WebClient.class);
        when(sharedBuilder.clone()).thenReturn(requestBuilder);
        when(requestBuilder.baseUrl("http://worker:8000")).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(webClient);

        WebClientFactory factory = new WebClientFactory(sharedBuilder);

        factory.forBaseUrl("http://worker:8000");

        verify(sharedBuilder).clone();
        verify(requestBuilder).baseUrl("http://worker:8000");
        verify(requestBuilder).build();
    }
}
