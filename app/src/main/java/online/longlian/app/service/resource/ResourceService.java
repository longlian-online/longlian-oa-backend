package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.FileProcessStatus;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.ResourceCreateParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourcCreateVO;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;

    private final StorageProperties storageProperties;

    public ResourcCreateVO create(ResourceCreateParamsBO params) {
        // 1. 生成文件ID
        long fileId = IdWorker.getId();

        // 2. 生成存储KEY
        String storageKey = buildStorageKey(params.getBizType(), fileId, params.getFileExt());

        // 3. 构建实体
        Resource file = Resource.builder()
                .id(fileId)
                .orgId(params.getOrgId())
                .storageType(storageProperties.getType())
                .storageKey(storageKey)
                .fileName(params.getFileName())
                .fileExt(params.getFileExt())
                .fileSize(params.getFileSize())
                .fileMime(params.getFileMime())
                .bizType(params.getBizType())
                .bizId(params.getBizId())
                .processStatus(FileProcessStatus.Pending)
                .isReferenced(Status.DISABLED)
                .creatorId(params.getCreatorId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. 获取上传链接
        StorageService storageService = storageFactory.get(file.getStorageType());
        PresignedUploadUrlResultBO uploadBO = storageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO(storageKey));

        resourceMapper.insert(file);

        return new ResourcCreateVO(fileId, uploadBO.getUploadUrl(), uploadBO.getKey(), file.getStorageType());
    }

    private String buildStorageKey(String bizType, Long fileId, String ext) {
        return String.format("%s.%s", Paths.get(bizType, String.valueOf(fileId)), ext);
    }

    public String getFileAccessUrl(Long resourceID) {
        Resource resource = resourceMapper.selectById(resourceID);
        StorageType type = resource.getStorageType();
        String key = resource.getStorageKey();
        String baseUrl = "";

        switch (type) {
            case LOCAL -> baseUrl = storageProperties.getLocal().getBaseUrl();
            case OSS -> baseUrl = storageProperties.getOss().getBaseUrl();
        };
        return Paths.get(baseUrl, key).toString();
    }
}
