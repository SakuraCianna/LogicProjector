package com.LogicProjector.exporttask.worker;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.LogicProjector.http.WebClientFactory;

@Component
public class HttpMediaExportWorkerClient implements MediaExportWorkerClient {

    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public HttpMediaExportWorkerClient(@Value("${pas.export.worker-base-url}") String workerBaseUrl,
            WebClientFactory webClientFactory,
            @Value("${pas.export.worker-timeout-seconds:30}") long timeoutSeconds) {
        this(webClientFactory.forBaseUrl(workerBaseUrl), Duration.ofSeconds(timeoutSeconds));
    }

    HttpMediaExportWorkerClient(WebClient webClient, Duration timeout) {
        this.webClient = webClient;
        this.timeout = timeout;
    }

    @Override
    public MediaExportWorkerResult createExport(MediaExportWorkerRequest request) {
        return webClient.post()
                .uri("/exports")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MediaExportWorkerResult.class)
                .block(timeout);
    }
}
