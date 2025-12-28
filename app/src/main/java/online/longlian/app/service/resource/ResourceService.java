package online.longlian.app.service.resource;

import cn.hutool.core.util.IdUtil;
import online.longlian.app.common.enumeration.ResourceStatus;
import online.longlian.app.common.enumeration.ResourceStorageType;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.dto.CreateResourceReq;
import online.longlian.app.pojo.dto.CreateResourceRes;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.bo.PresignedUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

    private final ResourceMapper resourceMapper;

    private final StorageServiceFactory storageFactory;

    @Value("${storage.type}")
    ResourceStorageType storageType;

    public ResourceService(ResourceMapper resourceMapper, StorageServiceFactory storageFactory) {
        this.resourceMapper = resourceMapper;
        this.storageFactory = storageFactory;
    }

    public CreateResourceRes create(CreateResourceReq req) {

        Resource resource = Resource.builder()
                .id(IdUtil.getSnowflakeNextId())  //雪花算法生成ID
                .storageType(storageType)
                .extend(req.getExt())
                .type(req.getType())
                .size(req.getSize())
                .status(ResourceStatus.Initial)
                .build();
        // 生成 key
        String key = buildResourceKey(resource);
        resource.setKey(key);
        // 生成预签名上传信息
        StorageService storageService = storageFactory.get(storageType);
        PresignedUpload upload = storageService.generatePresignedUpload(resource);

        resourceMapper.insert(resource);

        return new CreateResourceRes(
                resource.getId().toString(),
                upload.getUploadUrl(),
                upload.getKey(),
                resource.getStorageType()
        );
    }

    private String buildResourceKey(Resource resource) {
        return "resource/"
                + resource.getType().name().toLowerCase()
                + "/"
                + resource.getId();
    }
}
