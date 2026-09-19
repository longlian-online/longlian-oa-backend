package online.longlian.app.controller.common;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.pojo.dto.common.LocalFileReadDTO;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.resource.LocalFileIngress;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageControllerTest {
    private final ResourceService resources = mock(ResourceService.class);
    private final LocalFileIngress ingress = mock(LocalFileIngress.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1_000L), ZoneOffset.UTC);
    private final FileStorageController controller = new FileStorageController(resources, ingress, storageProperties, clock);

    @Test
    void shouldReturnResourceForSignedReadRequest() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{7});
        LocalFileReadDTO dto = signedDto();
        when(ingress.read(anyParams())).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> response = controller.readLocalFile(dto, null);

        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(ingress).read(new LocalFileReadParamsBO("avatar/1.png", 1_000L, "a".repeat(64)));
    }

    /** CDN 回源响应只在剩余鉴权期内允许浏览器复用本地缓存。 */
    @Test
    void shouldCacheCdnLocalReadUntilAuthenticationExpires() {
        StorageProperties.CdnConfig cdn = new StorageProperties.CdnConfig();
        cdn.setEnabled(true);
        cdn.setAuthTtlSeconds(300);
        storageProperties.setCdn(cdn);
        LocalFileReadDTO dto = signedDto();
        dto.setExpires(1_260L);
        when(ingress.read(anyParams())).thenReturn(new ByteArrayResource(new byte[]{7}));

        ResponseEntity<org.springframework.core.io.Resource> response = controller.readLocalFile(dto, 960L);

        assertThat(response.getHeaders().getCacheControl()).contains("max-age=260", "must-revalidate");
    }

    @Test
    void shouldForwardUploadMetadataAndSession() {
        CreateFileReqDTO request = new CreateFileReqDTO("avatar.png", "png", 7L, "image/png", "avatar");
        ResourceCreateVO expected = new ResourceCreateVO(1L, "upload", "avatar/1.png", StorageType.LOCAL);
        when(resources.create(argThat(params -> params.getCreatorId().equals(5L)
                && params.getOrgId().equals(6L)
                && params.getFileName().equals("avatar.png")))).thenReturn(expected);

        assertThat(controller.createFileUpload(request, new SessionContext(5L, 6L))).isSameAs(expected);
    }

    @Test
    void shouldFailWhenRequestContentCannotBeRead() throws IOException {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        doThrow(new IOException("read failed")).when(request).getInputStream();

        assertThat(catchThrowable(() -> controller.uploadLocalFile(signedDto(), request)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法读取上传内容");
    }

    private static LocalFileReadDTO signedDto() {
        LocalFileReadDTO dto = new LocalFileReadDTO();
        dto.setKey("avatar/1.png");
        dto.setExpires(1_000L);
        dto.setSignature("a".repeat(64));
        return dto;
    }

    private static LocalFileReadParamsBO anyParams() {
        return org.mockito.ArgumentMatchers.any(LocalFileReadParamsBO.class);
    }
}
