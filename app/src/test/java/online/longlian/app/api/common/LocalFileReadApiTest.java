package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.resource.impl.LocalStorageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.UriUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@TestPropertySource(properties = {
        "storage.type=LOCAL", "storage.local.base-url=",
        "storage.local.directory=${java.io.tmpdir}/longlian-issue107-${random.uuid}"
})
class LocalFileReadApiTest extends BaseApiTest {
    @Autowired private ResourceService resources;
    @Autowired private LocalStorageService localStorage;
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
        LocalFileReadParamsBO signed = signer.sign("avatar/1.png");
        for (long expires : new long[]{1L, signed.expires() + 1}) {
            request().queryParam("key", signed.key()).queryParam("expires", expires)
                    .queryParam("signature", signed.signature()).get("/common/file/local").then()
                    .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
    }

    /** 有效签名不能恢复已删除的资源元数据。 */
    @Test
    void shouldRejectMissingResource() {
        LocalFileReadParamsBO signed = signer.sign("avatar/1.png");
        request().queryParam("key", signed.key()).queryParam("expires", signed.expires())
                .queryParam("signature", signed.signature()).get("/common/file/local").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /** 只写盘，不激活。 */
    @Test
    void shouldUploadValidLocalImageSuccessfully() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);

        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Integer processStatus = jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE storage_key = ?", Integer.class, created.key());
        assertThat(processStatus).isZero();
        Long unboundBizId = jdbcTemplate.queryForObject(
                "SELECT biz_id FROM resource WHERE storage_key = ?", Long.class, created.key());
        assertThat(unboundBizId).isZero();
        assertThat(Files.readAllBytes(Path.of(properties.getLocal().getDirectory()).resolve(created.key()))).isEqualTo(png);
    }

    /** 声明为图片但内容无法解码时拒绝上传，并保留待上传状态。 */
    @Test
    void shouldRejectFakeLocalImage() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] content = "not-an-image".getBytes(StandardCharsets.UTF_8);
        LocalUpload created = createLocalUpload(token, content.length);

        putSignedUpload(created.uploadUrl(), content)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        Integer processStatus = jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE storage_key = ?", Integer.class, created.key());
        assertThat(processStatus).isZero();
        assertThat(Path.of(properties.getLocal().getDirectory()).resolve(created.key())).doesNotExist();
    }

    /** 已完成的本地文件不能使用同一存储 key 再次覆盖。 */
    @Test
    void shouldRejectRepeatedLocalUpload() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);
        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));

        assertThat(Files.readAllBytes(Path.of(properties.getLocal().getDirectory()).resolve(created.key()))).isEqualTo(png);
    }

    /** 请求体大小与创建上传时声明的大小不一致时拒绝保存。 */
    @Test
    void shouldRejectLocalUploadWithMismatchedSize() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length + 1L);

        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(Path.of(properties.getLocal().getDirectory()).resolve(created.key())).doesNotExist();
    }

    /** 不登录且缺少签名参数时不能上传。 */
    @Test
    void shouldRejectLocalUploadWithoutAuthentication() {
        request().contentType("application/octet-stream").queryParam("key", "avatar/1.png")
                .body(new byte[]{1}).put("/common/file/local").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 只写盘，不激活。登录只用于创建上传；随后无 token 即可 PUT 签名地址。 */
    @Test
    void shouldUploadLocalFileUsingSignedUrlWithoutLogin() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);

        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Integer processStatus = jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE storage_key = ?", Integer.class, created.key());
        assertThat(processStatus).isZero();
        assertThat(Files.readAllBytes(Path.of(properties.getLocal().getDirectory()).resolve(created.key()))).isEqualTo(png);
    }

    /** PUT 后、绑定前，读域签名不能读取 Pending 资源。 */
    @Test
    void shouldRejectReadBeforeResourceBound() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);
        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        LocalFileReadParamsBO read = signer.sign(created.key());
        String readUrl = "/common/file/local?key="
                + UriUtils.encodeQueryParam(created.key(), StandardCharsets.UTF_8)
                + "&expires=" + read.expires()
                + "&signature=" + read.signature();

        request().urlEncodingEnabled(false).get(readUrl).then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /** 绑定用户头像后资源才 Activated，业务返回的 avatarUrl 可读原图。 */
    @Test
    void shouldActivateResourceWhenBoundToUser() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);
        putSignedUpload(created.uploadUrl(), png)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Integer processStatus = jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE storage_key = ?", Integer.class, created.key());
        Long unboundBizId = jdbcTemplate.queryForObject(
                "SELECT biz_id FROM resource WHERE storage_key = ?", Long.class, created.key());
        assertThat(processStatus).isZero();
        assertThat(unboundBizId).isZero();

        authRequest(token)
                .body(Map.of("nickname", "user", "avatarFileId", created.fileId()))
                .put("/app/user/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Integer boundStatus = jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE storage_key = ?", Integer.class, created.key());
        Long bizId = jdbcTemplate.queryForObject(
                "SELECT biz_id FROM resource WHERE storage_key = ?", Long.class, created.key());
        assertThat(boundStatus).isEqualTo(1);
        assertThat(bizId).isEqualTo(1L);

        String avatarUrl = authRequest(token).get("/app/user/").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).extract().path("data.avatarUrl");
        byte[] content = request().urlEncodingEnabled(false).get(avatarUrl).then()
                .statusCode(200).extract().asByteArray();
        assertThat(content).isEqualTo(png);
    }

    /** 读取签名不能授权上传。 */
    @Test
    void shouldRejectReadSignatureOnLocalUpload() throws IOException {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        byte[] png = createPng();
        LocalUpload created = createLocalUpload(token, png.length);
        LocalFileReadParamsBO read = signer.sign(created.key());
        String readSignedUrl = "/common/file/local?key="
                + UriUtils.encodeQueryParam(created.key(), StandardCharsets.UTF_8)
                + "&expires=" + read.expires()
                + "&signature=" + read.signature();

        putSignedUpload(readSignedUrl, png)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 文件大小必须为正数，避免创建永远无法完成的上传记录。 */
    @Test
    void shouldRejectCreateLocalUploadWithZeroSize() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        CreateFileReqDTO request = new CreateFileReqDTO(
                "avatar.png", "png", 0L, "image/png", "avatar");

        authRequest(token).body(request).post("/common/file/upload").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    private void createFile() {
        createResource(1L, 1L, 1L);
        // 数据库在每个用例前清空，固定 key 的本地文件也需同步清理以保持测试隔离。
        localStorage.delete("avatar/1.png");
        localStorage.upload(LocalFileWriteParamsBO.builder()
                .storageKey("avatar/1.png")
                .content(new ByteArrayInputStream(new byte[]{7}))
                .expectedSize(1L)
                .build());
    }

    private LocalUpload createLocalUpload(String token, long fileSize) {
        CreateFileReqDTO request = new CreateFileReqDTO(
                "avatar.png", "png", fileSize, "image/png", "avatar");
        var response = authRequest(token).body(request).post("/common/file/upload").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .extract();
        String uploadUrl = response.path("data.uploadUrl");
        assertThat(uploadUrl).contains("expires=", "signature=");
        return new LocalUpload(response.path("data.fileId"), response.path("data.key"), uploadUrl);
    }

    private io.restassured.response.ValidatableResponse putSignedUpload(String uploadUrl, byte[] body) {
        return request().urlEncodingEnabled(false)
                .contentType("application/octet-stream")
                .body(body)
                .put(uploadUrl)
                .then();
    }

    private byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    @AfterAll
    void cleanFiles() throws Exception {
        Path root = Path.of(properties.getLocal().getDirectory());
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record LocalUpload(String fileId, String key, String uploadUrl) {
    }
}
