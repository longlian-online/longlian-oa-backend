package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@TestPropertySource(properties = {
        "storage.type=COS",
        "storage.cos.bucket=test-bucket",
        "storage.cos.region=ap-guangzhou",
        "storage.cos.secret-id=test-secret-id",
        "storage.cos.secret-key=test-secret-key",
        "storage.cdn.enabled=true",
        "storage.cdn.url-prefix=https://static.example.com",
        "storage.cdn.auth-key=test-cdn-key"
})
class CdnReadUrlApiTest extends BaseApiTest {

    /** COS 私有资源经业务接口返回 CDN 路径令牌链接，上传凭据不暴露给读取接口。 */
    @Test
    void shouldReturnCdnSignedUrlForCosAvatar() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET storage_type = 3 WHERE id = 1");
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 1 WHERE id = 1");

        String token = loginAs("user", "123456");
        String readUrl = authRequest(token).get("/app/user/").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .extract().path("data.avatarUrl");

        URI uri = URI.create(readUrl);
        String timestamp = queryValue(uri.getRawQuery(), "t");
        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("static.example.com");
        assertThat(uri.getRawPath()).isEqualTo("/avatar/1.png");
        assertThat(queryValue(uri.getRawQuery(), "token"))
                .isEqualTo(md5("test-cdn-key" + uri.getRawPath() + timestamp));
    }

    private String queryValue(String query, String name) {
        return java.util.Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair[0].equals(name))
                .map(pair -> pair[1])
                .findFirst()
                .orElseThrow();
    }

    private String md5(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
