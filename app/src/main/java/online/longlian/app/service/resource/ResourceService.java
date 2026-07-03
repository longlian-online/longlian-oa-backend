package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;

    private final StorageProperties storageProperties;

    public ResourceCreateVO create(ResourceCreateParamsBO params) {
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
                .creatorId(params.getCreatorId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. 获取上传链接
        StorageService storageService = storageFactory.get(file.getStorageType());
        PresignedUploadUrlResultBO uploadBO = storageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO(storageKey));

        resourceMapper.insert(file);

        return new ResourceCreateVO(fileId, uploadBO.getUploadUrl(), uploadBO.getKey(), file.getStorageType());
    }

    public String getResourceReadUrl(Long fileId) {
        Map<Long, ResourceReadUrlGetResultBO> resourceMap = this.getResourceReadUrls(List.of(fileId));
        if (resourceMap.size() != 1 && resourceMap.get(fileId) == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }

        return resourceMap.get(fileId).getUrl();
    }

    /**
     * 获取资源读取链接
     * <p><strong>调用方须自行检查是否有权限读取该资源</strong></p>
     * @return key: 资源ID, value: 资源读取链接 BO 对象
     *
     */
    public Map<Long, ResourceReadUrlGetResultBO> getResourceReadUrls(List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<Resource> query = lambdaQuery(Resource.class).select(
                Resource::getId,
                Resource::getStorageKey,
                Resource::getStorageType,
                Resource::getOrgId
        ).in(Resource::getId, resourceIds);

        List<Resource> resources = resourceMapper.selectList(query);

        Map<String, Long> resourceIdKeyMap = resources.stream().collect(Collectors.toMap(Resource::getStorageKey, Resource::getId));

        Stream<ResourceReadUrlGetResultBO> resourceReadUrlResultStream = resources.stream().map(resource -> {
            StorageService storageService = storageFactory.get(resource.getStorageType());
            String resourceReadUrl = storageService.getResourceReadUrl(resource.getStorageKey());

            return new ResourceReadUrlGetResultBO(
                    resourceReadUrl,
                    resource.getOrgId(),
                    resource.getStorageKey()
            );
        });

        return resourceReadUrlResultStream.collect(Collectors.toMap((org) -> resourceIdKeyMap.get(org.getKey()), org -> org));
    }

    /**
     * 回填资源的业务对象 ID。
     * <p>
     * 用于先上传文件、后创建业务对象的场景（如创建企划时先上传封面图），
     * 在业务对象创建完成后将业务 ID 回填到已关联的资源记录上，以便后续按业务对象做资源清理。
     *
     * @param resourceId 资源 ID
     * @param bizId      业务对象 ID
     */
    public void bindBizId(Long resourceId, Long bizId) {
        if (resourceId == null || bizId == null) {
            return;
        }
        resourceMapper.update(null,
                new LambdaUpdateWrapper<Resource>()
                        .eq(Resource::getId, resourceId)
                        .set(Resource::getBizId, bizId));
    }

    private String buildStorageKey(String bizType, Long fileId, String ext) {
        return String.format("%s.%s", Paths.get(bizType, String.valueOf(fileId)), ext);
    }

    private String buildFileAccessUrl(Resource resource) {
        if (resource == null || resource.getStorageType() == null || resource.getStorageKey() == null) {
            return null;
        }
        StorageType type = resource.getStorageType();
        String key = resource.getStorageKey();
        String baseUrl = "";

        switch (type) {
            case LOCAL -> baseUrl = storageProperties.getLocal().getBaseUrl();
            case OSS -> baseUrl = storageProperties.getOss().getBaseUrl();
        }
        return Paths.get(baseUrl, key).toString();
    }
}
