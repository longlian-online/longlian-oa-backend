package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/** 任务附件创建上传必须携带原文件名的 Content-Disposition，头像封面不带。 */
@TestPropertySource(properties = {
        "storage.type=COS",
        "storage.cos.bucket=test-bucket",
        "storage.cos.region=ap-guangzhou",
        "storage.cos.secret-id=test-secret-id",
        "storage.cos.secret-key=test-secret-key",
        "longlian.server-url="
})
class ContentDispositionUploadApiTest extends BaseApiTest {

    @Test
    void shouldReturnSignedDispositionForTaskSubmitUpload() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        CreateFileReqDTO request = new CreateFileReqDTO(
                "我的文件.docx", "docx", 100L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "task_submit");

        authRequest(token).body(request).post("/common/file/upload").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.contentDisposition",
                        containsString("attachment; filename=\"____.docx\"; filename*=UTF-8''%E6%88%91"));
    }

    @Test
    void shouldOmitDispositionForAvatarUpload() {
        createUserWithOrganization(1L, "user", "123456", "user@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("user", "123456");
        CreateFileReqDTO request = new CreateFileReqDTO("avatar.png", "png", 100L, "image/png", "avatar");

        authRequest(token).body(request).post("/common/file/upload").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.contentDisposition", nullValue());
    }
}
