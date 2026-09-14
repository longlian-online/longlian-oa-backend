package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.resource.StorageServiceFactory;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@TestPropertySource(properties = {
        "storage.type=LOCAL", "storage.local.base-url=",
        "storage.local.directory=${java.io.tmpdir}/longlian-issue107-${random.uuid}"
})
class LocalFileReadApiTest extends BaseApiTest {
    @Autowired private ResourceService resources;
    @Autowired private StorageServiceFactory storage;
    @Autowired private StorageProperties properties;
    @Autowired private LocalFileUrlSigner signer;

    /** 业务接口返回的授权链接无需 Authorization 请求头即可读取图片。 */
    @Test
    void shouldReadAvatarUsingSignedBusinessUrl() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        createFile();
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 1 WHERE id = 1");
        String token = loginAs("user", "123456");
        String url = authRequest(token).get("/app/user/").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).extract().path("data.avatarUrl");
        assertThat(url).contains("expires=", "signature=");
        byte[] content = request().urlEncodingEnabled(false).get(url).then().statusCode(200).extract().asByteArray();
        assertThat(content).containsExactly(7);
    }

    /** 仅知道存储 key 不能直接获得文件读取权限。 */
    @Test
    void shouldRejectUnsignedRead() {
        createFile();
        request().queryParam("key", "avatar/1.png").get("/common/file/local").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 一个资源的签名不能读取其他资源，也不能跨组织读取文件。 */
    @Test
    void shouldRejectCrossResourceRead() {
        createFile();
        createResource(2L, 2L, 2L);
        String url = resources.getResourceReadUrl(1L).replace("avatar/1.png", "avatar/2.png");
        request().urlEncodingEnabled(false).get(url).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 修改过期时间会使签名失效，已过期的链接同样必须拒绝。 */
    @Test
    void shouldRejectInvalidExpiryAndSignature() {
        createFile();
        var signed = signer.sign("avatar/1.png");
        for (long expires : new long[]{1L, signed.expires() + 1}) {
            request().queryParam("key", signed.key()).queryParam("expires", expires)
                    .queryParam("signature", signed.signature()).get("/common/file/local").then()
                    .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
    }

    /** 有效签名不能恢复已删除的资源元数据。 */
    @Test
    void shouldRejectMissingResource() {
        var signed = signer.sign("avatar/1.png");
        request().queryParam("key", signed.key()).queryParam("expires", signed.expires())
                .queryParam("signature", signed.signature()).get("/common/file/local").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    private void createFile() {
        createResource(1L, 1L, 1L);
        storage.get(StorageType.LOCAL).upload("avatar/1.png", new byte[]{7});
    }

    @AfterAll
    void cleanFiles() throws Exception {
        Path root = Path.of(properties.getLocal().getDirectory());
        Files.deleteIfExists(root.resolve("avatar/1.png"));
        Files.deleteIfExists(root.resolve("avatar"));
        Files.deleteIfExists(root);
    }
}
