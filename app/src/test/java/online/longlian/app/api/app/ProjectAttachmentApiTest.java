package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 企划附件接口测试。
 * <p>
 * 覆盖绑定、列表、下载、删除的成功路径与权限/可见性失败路径，
 * 并验证删除与企划整体删除都会把文件送入资源保留期。
 * 本地上传地址依赖空 server-url 生成相对路径，存储目录按用例隔离。
 */
@TestPropertySource(properties = {
        "longlian.server-url=",
        "storage.local.directory=${java.io.tmpdir}/longlian-lon15-attachment-${random.uuid}"
})
class ProjectAttachmentApiTest extends BaseApiTest {

    private static final String ATTACHMENTS = "/app/projects/%d/attachments";

    @Autowired
    private StorageProperties storageProperties;

    /** 负责人完成「上传 → 绑定 → 列表 → 下载 → 删除」全流程。 */
    @Test
    void shouldBindListDownloadAndDeleteAttachment() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        byte[] content = "attachment-body".getBytes(StandardCharsets.UTF_8);
        String fileId = uploadAttachment(token, 1L, "企划设定.pdf", "pdf", "application/pdf", content);

        authRequest(token).body(Map.of("fileId", fileId))
                .post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Response list = authRequest(token).get(ATTACHMENTS.formatted(1L));
        list.then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(1))
                .body("data[0].fileId", equalTo(fileId))
                .body("data[0].fileName", equalTo("企划设定.pdf"))
                .body("data[0].fileSize", equalTo(content.length))
                .body("data[0].fileMime", equalTo("application/pdf"))
                .body("data[0].uploaderId", equalTo("1"))
                .body("data[0].uploaderName", equalTo("creator"))
                .body("data[0].createdAt", notNullValue());
        String attachmentId = list.path("data[0].id").toString();
        assertThat(attachmentId).isNotBlank();
        // 列表只输出元信息，不返回任何读取地址。
        assertThat(list.asString()).doesNotContain("readUrl").doesNotContain("token=");

        Response download = authRequest(token).get(ATTACHMENTS.formatted(1L) + "/" + attachmentId + "/download");
        download.then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.readUrl", notNullValue())
                .body("data.expireAt", notNullValue());
        String readUrl = download.path("data.readUrl");
        assertThat(download.jsonPath().getLong("data.expireAt")).isGreaterThan(Instant.now().getEpochSecond());
        Response raw = request().urlEncodingEnabled(false).get(readUrl);
        raw.then().statusCode(200);
        assertThat(raw.getHeader("Content-Disposition"))
                .contains("attachment; filename=", "filename*=UTF-8''");
        assertThat(raw.asByteArray()).isEqualTo(content);

        authRequest(token).delete(ATTACHMENTS.formatted(1L) + "/" + attachmentId).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM project_attachment WHERE id = ?", Object.class, Long.valueOf(attachmentId)))
                .isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = ?", Integer.class, Long.valueOf(fileId)))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
    }

    /** 列表按上传时间新到旧，同一时间戳按附件 ID 倒序。 */
    @Test
    void shouldListAttachmentsNewestFirst() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        String first = uploadAttachment(token, 1L, "第一版.pdf", "pdf", "application/pdf", "one".getBytes(StandardCharsets.UTF_8));
        String second = uploadAttachment(token, 1L, "第二版.pdf", "pdf", "application/pdf", "two".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, first);
        bind(token, 1L, second);

        authRequest(token).get(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(2))
                .body("data[0].fileId", equalTo(second))
                .body("data[1].fileId", equalTo(first));
    }

    /** 上传业务类型白名单接受 project_attachment，且按普通文件 50MB 上限校验。 */
    @Test
    void shouldAcceptProjectAttachmentUpload() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("creator", "123456");

        authRequest(token).body(new CreateFileReqDTO(
                        "资料.pdf", "pdf", 50L * 1024 * 1024, "application/pdf", "project_attachment"))
                .post("/common/file/upload").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldRejectBindWithoutAuth() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");

        request().body(Map.of("fileId", "1")).post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectBindWithoutFileId() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");

        authRequest(token).body(Map.of()).post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 本组织管理员可绑定并删除他人企划附件，不改变企划归属。 */
    @ParameterizedTest
    @ValueSource(strings = {"ORG_ADMIN", "ORG_OWNER"})
    void shouldAllowOrganizationAdministratorToManageAttachments(String role) throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        createProject(1L, 1L, 1L, 1L, "企划一");
        createTestUser(2L, "editor", "123456", "editor@example.com");
        createOrganizationMember(2L, 1L, 2L, role);
        String token = loginAs("editor", "123456");
        String fileId = uploadAttachment(token, 1L, "管理员.pdf", "pdf", "application/pdf",
                "admin".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, fileId);
        Response list = authRequest(token, 1L).get(ATTACHMENTS.formatted(1L));
        list.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(1)).body("data[0].fileId", equalTo(fileId));
        String attachmentId = list.path("data[0].id").toString();
        authRequest(token, 1L).delete(ATTACHMENTS.formatted(1L) + "/" + attachmentId).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM project_attachment WHERE id=?",
                Object.class, Long.valueOf(attachmentId))).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT creator_id FROM project WHERE id=1", Long.class)).isEqualTo(1L);
    }

    /** 普通组员不能编辑其他创建者的企划附件。 */
    @Test
    void shouldRejectBindForNonCreator() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 2");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("member", "123456");
        String fileId = uploadAttachment(token, 1L, "成员文件.pdf", "pdf", "application/pdf",
                "member".getBytes(StandardCharsets.UTF_8));

        authRequest(token, 1L).body(Map.of("fileId", fileId)).post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_attachment", Long.class)).isZero();
    }

    @Test
    void shouldRejectBindForUnknownFile() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");

        authRequest(token).body(Map.of("fileId", "999999"))
                .post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 已绑定的文件不能再次绑定到同一企划。 */
    @Test
    void shouldRejectAlreadyBoundFile() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        String fileId = uploadAttachment(token, 1L, "企划设定.pdf", "pdf", "application/pdf",
                "body".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, fileId);

        authRequest(token).body(Map.of("fileId", fileId)).post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_attachment", Long.class)).isEqualTo(1L);
    }

    /** 上传文件归属组织与企划组织不一致时拒绝绑定。 */
    @Test
    void shouldRejectBindWithForeignOrganizationFile() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");
        createProject(1L, 2L, 2L, 1L, "组织2企划");
        String token = loginAs("creator", "123456");
        String fileId = uploadAttachment(token, 1L, "组织1文件.pdf", "pdf", "application/pdf",
                "org1".getBytes(StandardCharsets.UTF_8));

        authRequest(token, 2L).body(Map.of("fileId", fileId)).post(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 列表与下载沿用企划详情的组织隔离，跨组织不可见。 */
    @Test
    void shouldRejectListAndDownloadForForeignOrganization() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        String fileId = uploadAttachment(token, 1L, "企划设定.pdf", "pdf", "application/pdf",
                "body".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, fileId);
        String attachmentId = authRequest(token).get(ATTACHMENTS.formatted(1L)).then()
                .extract().path("data[0].id").toString();

        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");

        authRequest(token, 2L).get(ATTACHMENTS.formatted(1L)).then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token, 2L).get(ATTACHMENTS.formatted(1L) + "/" + attachmentId + "/download").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldRejectDownloadForDeletedAttachment() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        String fileId = uploadAttachment(token, 1L, "企划设定.pdf", "pdf", "application/pdf",
                "body".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, fileId);
        String attachmentId = authRequest(token).get(ATTACHMENTS.formatted(1L)).then()
                .extract().path("data[0].id").toString();
        authRequest(token).delete(ATTACHMENTS.formatted(1L) + "/" + attachmentId).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).get(ATTACHMENTS.formatted(1L) + "/" + attachmentId + "/download").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /** 企划整体删除时，附件关联逻辑删除，文件进入资源保留期。 */
    @Test
    void shouldAbandonAttachmentsWhenProjectDeleted() throws IOException {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_ADMIN");
        createProject(1L, 1L, 1L, 1L, "企划一");
        String token = loginAs("creator", "123456");
        String fileId = uploadAttachment(token, 1L, "企划设定.pdf", "pdf", "application/pdf",
                "body".getBytes(StandardCharsets.UTF_8));
        bind(token, 1L, fileId);

        authRequest(token).delete("/app/projects/1").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_attachment WHERE deleted_at IS NULL", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = ?", Integer.class, Long.valueOf(fileId)))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
    }

    /** 上传预签名文件并返回文件 ID，供绑定使用。 */
    private String uploadAttachment(String token, long orgId, String fileName, String ext, String mime,
                                    byte[] content) throws IOException {
        CreateFileReqDTO request = new CreateFileReqDTO(
                fileName, ext, (long) content.length, mime, "project_attachment");
        Response created = authRequest(token, orgId).body(request).post("/common/file/upload");
        created.then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String fileId = created.path("data.fileId").toString();
        String uploadUrl = created.path("data.uploadUrl");
        assertThat(uploadUrl).contains("expires=", "signature=");
        request().urlEncodingEnabled(false).contentType("application/octet-stream").body(content)
                .put(uploadUrl).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        return fileId;
    }

    private void bind(String token, long projectId, String fileId) {
        authRequest(token, 1L).body(Map.of("fileId", fileId))
                .post(ATTACHMENTS.formatted(projectId)).then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    private void createProject(long projectId, long orgId, long typeId, long creatorId, String title) {
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, creator_id) VALUES (?, ?, ?, ?)",
                typeId, orgId, "类型" + typeId, creatorId);
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (?, ?, ?, ?, ?)",
                projectId, orgId, typeId, title, creatorId);
    }

    @AfterAll
    void cleanFiles() throws Exception {
        Path root = Path.of(storageProperties.getLocal().getDirectory());
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        }
    }
}
