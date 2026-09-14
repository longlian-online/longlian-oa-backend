package online.longlian.app.service.resource;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalFileReadService {
    private final LocalFileUrlSigner signer;
    private final ResourceService resourceService;

    public Resource read(LocalFileReadParamsBO params) {
        signer.verify(params);
        return resourceService.getLocalResource(params.key());
    }
}
