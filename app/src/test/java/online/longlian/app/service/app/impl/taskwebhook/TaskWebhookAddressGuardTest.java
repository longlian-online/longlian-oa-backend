package online.longlian.app.service.app.impl.taskwebhook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskWebhookAddressGuardTest {
    private final TaskWebhookAddressGuard guard = new TaskWebhookAddressGuard();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com/hook",
            "https://user:secret@example.com/hook",
            "https://example.com:8443/hook",
            "https://127.0.0.1/hook",
            "https://[::ffff:127.0.0.1]/hook",
            "https://[fc00::1]/hook",
            "https://0.0.0.0/hook",
            "not a url",
            "https://10.1.2.3/hook",
            "https://192.168.0.8/hook",
            "https://169.254.169.254/hook",
            "https://[::1]/hook",
            "https://localhost/hook",
            "https://100.64.0.1/hook"
    })
    void shouldRejectAddressesThatAreNotPublicHttps(String url) {
        assertThatThrownBy(() -> guard.require(url)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldTreatAnUnresolvableNameAsTemporary() {
        assertThatThrownBy(() -> guard.require("https://webhook.invalid/hook"))
                .isInstanceOf(TaskWebhookTransientException.class);
    }

    @Test
    void shouldAcceptAPublicHttpsAddress() {
        assertThatCode(() -> guard.require("https://example.com/hooks/task")).doesNotThrowAnyException();
    }
}
