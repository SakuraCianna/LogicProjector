package com.LogicProjector.exporttask.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HttpMediaExportWorkerClient implements MediaExportWorkerClient {

    private final WebClient webClient;

    public HttpMediaExportWorkerClient(@Value("${pas.export.worker-base-url}") String workerBaseUrl) {
        this.webClient = WebClient.builder().baseUrl(workerBaseUrl).build();
    }

    @Override
    public MediaExportWorkerResult createExport(MediaExportWorkerRequest request) {
        return webClient.post()
                .uri("/exports")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MediaExportWorkerResult.class)
                .block();
    }
}
