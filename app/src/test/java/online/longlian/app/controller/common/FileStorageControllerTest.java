package online.longlian.app.controller.common;

import online.longlian.app.pojo.dto.common.LocalFileReadDTO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.resource.LocalFileReadService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

class FileStorageControllerTest {
    private final ResourceService resources = mock(ResourceService.class);
    private final LocalFileReadService reader = mock(LocalFileReadService.class);
    private final FileStorageController controller = new FileStorageController(resources, reader);

    @Test
    void shouldReturnResourceForSignedReadRequest() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{7});
        LocalFileReadDTO dto = new LocalFileReadDTO();
        dto.setKey("avatar/1.png");
        dto.setExpires(1_000L);
        dto.setSignature("a".repeat(64));
        when(reader.read(anyParams())).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> response = controller.readLocalFile(dto);

        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void shouldForwardUploadMetadataAndSession() {
        CreateFileReqDTO request = new CreateFileReqDTO("avatar.png", "png", 7L, "image/png", "avatar", 9L);
        ResourceCreateVO expected = new ResourceCreateVO(1L, "upload", "avatar/1.png", StorageType.LOCAL);
        when(resources.create(argThat(params -> params.getCreatorId().equals(5L)
                && params.getOrgId().equals(6L)
                && params.getFileName().equals("avatar.png")))).thenReturn(expected);

        assertThat(controller.createFileUpload(request, new SessionContext(5L, 6L))).isSameAs(expected);
    }

    @Test
    void shouldForwardLocalUploadContentAndSession() {
        byte[] content = {1, 2, 3};

        controller.uploadLocalFile("avatar/1.png", content, new SessionContext(5L, 6L));

        verify(resources).uploadLocalResource(argThat(params -> params.getStorageKey().equals("avatar/1.png")
                && params.getContent() == content
                && params.getUserId().equals(5L)
                && params.getOrgId().equals(6L)));
    }

    private static online.longlian.app.pojo.bo.common.LocalFileReadParamsBO anyParams() {
        return org.mockito.ArgumentMatchers.any(online.longlian.app.pojo.bo.common.LocalFileReadParamsBO.class);
    }
}
