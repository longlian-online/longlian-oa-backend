package online.longlian.app.controller.common;

import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileUploadParamsBO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.pojo.dto.common.LocalFileReadDTO;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageControllerTest {
    private final ResourceService resources = mock(ResourceService.class);
    private final FileStorageController controller = new FileStorageController(resources);

    @Test
    void shouldReturnResourceForSignedReadRequest() {
        ByteArrayResource resource = new ByteArrayResource(new byte[]{7});
        LocalFileReadDTO dto = new LocalFileReadDTO();
        dto.setKey("avatar/1.png");
        dto.setExpires(1_000L);
        dto.setSignature("a".repeat(64));
        when(resources.readLocalResource(anyParams())).thenReturn(resource);

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
    void shouldForwardLocalUploadContentAndSession() throws IOException {
        byte[] content = {1, 2, 3};
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(content);
        request.addHeader("Content-Length", content.length);

        controller.uploadLocalFile("avatar/1.png", request, new SessionContext(5L, 6L));

        ArgumentCaptor<LocalFileUploadParamsBO> captor = ArgumentCaptor.forClass(LocalFileUploadParamsBO.class);
        verify(resources).uploadLocalResource(captor.capture());
        LocalFileUploadParamsBO params = captor.getValue();
        assertThat(params.getStorageKey()).isEqualTo("avatar/1.png");
        assertThat(params.getContentLength()).isEqualTo(3L);
        assertThat(readContent(params.getContent())).isEqualTo(content);
        assertThat(params.getUserId()).isEqualTo(5L);
        assertThat(params.getOrgId()).isEqualTo(6L);
    }

    @Test
    void shouldFailWhenRequestContentCannotBeRead() throws IOException {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        doThrow(new IOException("read failed")).when(request).getInputStream();

        assertThat(catchThrowable(
                () -> controller.uploadLocalFile("avatar/1.png", request, new SessionContext(5L, 6L))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法读取上传内容");
    }

    private static byte[] readContent(InputStream content) {
        try {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static LocalFileReadParamsBO anyParams() {
        return org.mockito.ArgumentMatchers.any(LocalFileReadParamsBO.class);
    }
}
