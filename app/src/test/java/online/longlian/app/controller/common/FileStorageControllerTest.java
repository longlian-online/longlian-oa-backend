package online.longlian.app.controller.common;

import online.longlian.app.pojo.dto.common.LocalFileReadDTO;
import online.longlian.app.service.resource.LocalFileReadService;
import online.longlian.app.service.resource.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageControllerTest {
    private final ResourceService resources = mock(ResourceService.class);
    private final LocalFileReadService reader = mock(LocalFileReadService.class);
    private final FileStorageController controller = new FileStorageController(resources, reader);

    @Test
    void shouldReturnResourceForSignedReadRequest() {
        var resource = new ByteArrayResource(new byte[]{7});
        var dto = new LocalFileReadDTO();
        dto.setKey("avatar/1.png");
        dto.setExpires(1_000L);
        dto.setSignature("a".repeat(64));
        when(reader.read(anyParams())).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> response = controller.readLocalFile(dto);

        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    private static online.longlian.app.pojo.bo.common.LocalFileReadParamsBO anyParams() {
        return org.mockito.ArgumentMatchers.any(online.longlian.app.pojo.bo.common.LocalFileReadParamsBO.class);
    }
}
