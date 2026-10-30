package online.longlian.app.service.app.impl.taskwebhook;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskWebhookHttpClientTest {
    private final TaskWebhookHttpClient client = new TaskWebhookHttpClient();

    @Test
    void shouldReturnTheStatusCodeWithoutFollowingRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            URI target = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/hook");
            assertThat(client.post(target, "{}".getBytes(StandardCharsets.UTF_8), "1", "2", "v1=abc"))
                    .isEqualTo(204);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRestoreTheInterruptWhenTheCallIsInterrupted() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> new TaskWebhookHttpClient(httpClient).post(
                URI.create("https://example.com/hook"), new byte[0], "1", "2", "v1=abc"))
                .isInstanceOf(TaskWebhookTransientException.class)
                .hasMessageContaining("被中断");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    void shouldTreatConnectionFailureAsRetryable() {
        assertThatThrownBy(() -> client.post(URI.create("http://127.0.0.1:1/hook"),
                new byte[0], "1", "2", "v1=abc"))
                .isInstanceOf(TaskWebhookTransientException.class)
                .hasMessageContaining("ConnectException");
    }
}
