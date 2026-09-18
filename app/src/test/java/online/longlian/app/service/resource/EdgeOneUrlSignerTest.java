package online.longlian.app.service.resource;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EdgeOneUrlSignerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1721029907L), ZoneOffset.UTC);

    /** EdgeOne 方法 D 必须以实际请求路径、当前时间戳和控制台密钥生成 token。 */
    @Test
    void shouldSignPrivateReadUrlUsingEdgeOneMethodD() {
        EdgeOneUrlSigner signer = new EdgeOneUrlSigner("https://static.example.com", "test-secret", CLOCK);

        assertThat(signer.sign("avatar/1.png"))
                .isEqualTo("https://static.example.com/avatar/1.png"
                        + "?token=81a97b30d25b4d66f2978240008a4430&t=1721029907");
    }

    /** 域名前缀可包含部署路径，签名必须覆盖该路径，避免节点按另一资源路径验签。 */
    @Test
    void shouldSignEncodedKeyBelowConfiguredPathPrefix() {
        EdgeOneUrlSigner signer = new EdgeOneUrlSigner("https://static.example.com/private/", "test-secret", CLOCK);

        assertThat(signer.sign("avatar/a b.png"))
                .isEqualTo("https://static.example.com/private/avatar/a%20b.png"
                        + "?token=fbcf558f94425fa0904705b197bf42a6&t=1721029907");
    }

    /** 缺少 EO 域名或密钥时不能退回 COS 读预签名链接，避免绕过 CDN 鉴权。 */
    @Test
    void shouldRejectMissingEdgeOneReadConfiguration() {
        EdgeOneUrlSigner signer = new EdgeOneUrlSigner("https://static.example.com", "", CLOCK);

        assertThatThrownBy(() -> signer.sign("avatar/1.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("COS 的 EdgeOne 读取域名和鉴权密钥必须配置");
    }

    /** 域名配置不能携带自己的参数，避免覆盖 EO 的 token 和 t 鉴权参数。 */
    @Test
    void shouldRejectEdgeOneDomainWithQueryParameters() {
        EdgeOneUrlSigner signer = new EdgeOneUrlSigner("https://static.example.com?token=bad", "test-secret", CLOCK);

        assertThatThrownBy(() -> signer.sign("avatar/1.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("COS 的 EdgeOne 读取域名必须是无查询参数的绝对 URL");
    }
}
