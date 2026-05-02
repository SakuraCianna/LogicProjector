package com.LogicProjector.exporttask.worker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class HttpMediaExportWorkerClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldTimeOutWhenWorkerDoesNotRespond() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/exports", new SlowHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        HttpMediaExportWorkerClient client = new HttpMediaExportWorkerClient(
                WebClient.builder().baseUrl(baseUrl).build(),
                Duration.ofMillis(50));

        assertThatThrownBy(() -> client.createExport(new MediaExportWorkerRequest(
                1L,
                2L,
                "QUICK_SORT",
                "summary",
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(),
                "class Demo {}",
                true,
                true)))
                .isInstanceOf(RuntimeException.class);
    }

    private static class SlowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            byte[] response = "{}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}
