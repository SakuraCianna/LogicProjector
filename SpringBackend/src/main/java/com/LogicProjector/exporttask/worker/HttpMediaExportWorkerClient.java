package com.LogicProjector.exporttask.worker;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HttpMediaExportWorkerClient implements MediaExportWorkerClient {

    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public HttpMediaExportWorkerClient(@Value("${pas.export.worker-base-url}") String workerBaseUrl,
            @Value("${pas.export.worker-timeout-seconds:30}") long timeoutSeconds) {
        this(workerBaseUrl, Duration.ofSeconds(timeoutSeconds));
    }

    HttpMediaExportWorkerClient(String workerBaseUrl, Duration timeout) {
        this.webClient = WebClient.builder().baseUrl(workerBaseUrl).build();
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
