package online.longlian.app.service.app.impl.taskwebhook;

import online.longlian.app.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebhookEndpointPolicyTest {
    private final TaskWebhookAddressGuard guard = mock(TaskWebhookAddressGuard.class);
    private final WebhookEndpointPolicy policy = new WebhookEndpointPolicy(guard);

    @Test
    void shouldClearSecretWhenUrlIsCleared() {
        WebhookEndpointPolicy.Endpoint endpoint = policy.resolve("  ", null, "0123456789abcdef");

        assertThat(endpoint.url()).isNull();
        assertThat(endpoint.secret()).isNull();
        verifyNoInteractions(guard);
    }

    @Test
    void shouldKeepExistingSecretWhenEditOmitsIt() {
        when(guard.require("https://example.com/hook")).thenReturn(URI.create("https://example.com/hook"));

        WebhookEndpointPolicy.Endpoint endpoint = policy.resolve(
                " https://example.com/hook ", " ", "0123456789abcdef");

        assertThat(endpoint.url()).isEqualTo("https://example.com/hook");
        assertThat(endpoint.secret()).isEqualTo("0123456789abcdef");
    }

    @Test
    void shouldRejectSecretThatIsTooShort() {
        when(guard.require("https://example.com/hook")).thenReturn(URI.create("https://example.com/hook"));

        assertThatThrownBy(() -> policy.resolve("https://example.com/hook", "short-secret", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("16 到 128");
    }

    @Test
    void shouldRejectSecretWithoutUrl() {
        assertThatThrownBy(() -> policy.resolve(null, "0123456789abcdef", null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("未填写 webhook 地址");
    }
}
