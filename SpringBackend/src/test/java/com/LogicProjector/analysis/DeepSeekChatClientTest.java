package com.LogicProjector.analysis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class DeepSeekChatClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldTimeOutWhenAiProviderDoesNotRespond() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", new SlowHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        DeepSeekChatClient client = new DeepSeekChatClient(
                baseUrl,
                "deepseek-chat",
                "test-key",
                "DEEPSEEK_API_KEY",
                new ObjectMapper(),
                Duration.ofMillis(50));

        assertThatThrownBy(() -> client.createStructuredResponse("system", "user"))
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
