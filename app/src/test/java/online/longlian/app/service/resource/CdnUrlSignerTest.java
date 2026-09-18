package online.longlian.app.service.resource;
import online.longlian.app.common.properties.StorageProperties;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CdnUrlSignerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1721029907L), ZoneOffset.UTC);

    /** CDN 路径令牌必须以实际请求路径、当前时间戳和配置密钥生成 token。 */
    @Test
    void shouldSignPrivateReadUrlUsingConfiguredPathTokenProtocol() {
        CdnUrlSigner signer = new CdnUrlSigner("https://static.example.com", "test-secret", CLOCK);

        assertThat(signer.sign("avatar/1.png"))
                .isEqualTo("https://static.example.com/avatar/1.png"
                        + "?token=81a97b30d25b4d66f2978240008a4430&t=1721029907");
    }

    /** 域名前缀可包含部署路径，签名必须覆盖该路径，避免节点按另一资源路径验签。 */
    @Test
    void shouldSignEncodedKeyBelowConfiguredPathPrefix() {
        CdnUrlSigner signer = new CdnUrlSigner("https://static.example.com/private/", "test-secret", CLOCK);

        assertThat(signer.sign("avatar/a b.png"))
                .isEqualTo("https://static.example.com/private/avatar/a%20b.png"
                        + "?token=fbcf558f94425fa0904705b197bf42a6&t=1721029907");
    }

    /** 本地回源的 key 等参数由本地 HMAC 保护，CDN 路径令牌只签回源路径。 */
    @Test
    void shouldAppendProtectedOriginQueryAfterCdnToken() {
        CdnUrlSigner signer = new CdnUrlSigner("https://static.example.com", "test-secret", CLOCK);

        assertThat(signer.signPath("/common/file/local", "key=avatar/1.png&expires=1721029908&signature=local-signature"))
                .isEqualTo("https://static.example.com/common/file/local?key=avatar/1.png"
                        + "&expires=1721029908&signature=local-signature"
                        + "&token=96f266ee47fcf9965117d2e095830f71&t=1721029907");
    }

    /** 配置对象和字符串构造路径生成相同的 CDN 读取协议。 */
    @Test
    void shouldSignUsingCdnConfiguration() {
        StorageProperties.CdnConfig config = new StorageProperties.CdnConfig();
        config.setUrlPrefix("https://static.example.com");
        config.setAuthKey("test-secret");

        assertThat(new CdnUrlSigner(config, CLOCK).sign("avatar/1.png"))
                .isEqualTo("https://static.example.com/avatar/1.png"
                        + "?token=81a97b30d25b4d66f2978240008a4430&t=1721029907");
    }

    /** 存储 key 和 CDN 路径必须保持路径语义，不能注入查询参数或片段。 */
    @Test
    void shouldRejectMalformedStorageKeysAndPaths() {
        CdnUrlSigner signer = new CdnUrlSigner("https://static.example.com", "test-secret", CLOCK);

        assertThatThrownBy(() -> signer.sign(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.sign("/avatar/1.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.signPath("avatar/1.png", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.signPath("/avatar/1.png?download=1", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.signPath("/avatar/1.png#fragment", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CdnUrlSigner("https://[invalid", "test-secret", CLOCK).sign("avatar/1.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CDN 读取域名必须是无查询参数的绝对 URL");
    }

    /** 缺少 CDN 域名或密钥时不能回退到存储服务读预签名链接。 */
    @Test
    void shouldRejectMissingCdnReadConfiguration() {
        CdnUrlSigner signer = new CdnUrlSigner((StorageProperties.CdnConfig) null, CLOCK);

        assertThatThrownBy(() -> signer.sign("avatar/1.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CDN 读取域名和鉴权密钥必须配置");
    }

    /** 域名配置不能携带自己的参数，避免覆盖 CDN 的 token 和 t 鉴权参数。 */
    @Test
    void shouldRejectCdnDomainWithQueryParameters() {
        CdnUrlSigner signer = new CdnUrlSigner("https://static.example.com?token=bad", "test-secret", CLOCK);

        assertThatThrownBy(() -> signer.sign("avatar/1.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CDN 读取域名必须是无查询参数的绝对 URL");
    }
}
