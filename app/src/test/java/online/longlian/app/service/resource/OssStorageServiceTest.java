package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.impl.OssStorageService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicReference;


@SpringBootTest
@ActiveProfiles
@Disabled
public class OssStorageServiceTest {

    @Autowired
    private OssStorageService ossStorageService;

    @Test
    void testGeneratePresignedUploadUrl() {
        AtomicReference<PresignedUploadUrlResultBO> result = new AtomicReference<>();
        Assertions.assertThatCode(()->{
            result.set(ossStorageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO("key")));
        }).doesNotThrowAnyException();

        Assertions.assertThat(result.get()).isNotNull();
        Assertions.assertThat(result.get().getUploadUrl()).isNotEmpty();
        Assertions.assertThat(result.get().getKey()).isNotEmpty();
    }
}
