package online.longlian.app.service.app.impl.taskwebhook;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class TaskWebhookHttpClient implements TaskWebhookClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public int post(URI target, byte[] body, String timestamp, String deliveryId, String signature) {
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Longlian-Timestamp", timestamp)
                .header("X-Longlian-Delivery", deliveryId)
                .header("X-Longlian-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TaskWebhookTransientException("webhook 投递被中断");
        } catch (IOException exception) {
            throw new TaskWebhookTransientException("webhook 投递失败: " + exception.getClass().getSimpleName());
        }
    }
}
