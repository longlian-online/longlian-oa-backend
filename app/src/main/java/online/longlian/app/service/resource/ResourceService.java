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
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;

    private final StorageProperties storageProperties;
    private final Clock clock;

    public ResourceCreateVO create(ResourceCreateParamsBO params) {
        if (isImageBusiness(params.getBizType()) && !params.getFileMime().startsWith("image/")) {
            throw new AppException(ResultCode.PARAM_ERROR, "头像和封面只能上传图片");
        }
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
                .bizId(0L)
                .processStatus(FileProcessStatus.Pending)
                .creatorId(params.getCreatorId())
                .createdAt(LocalDateTime.now(clock))
                .updatedAt(LocalDateTime.now(clock))
                .build();

        // 4. 获取上传链接
        StorageService storageService = storageFactory.get(file.getStorageType());
        PresignedUploadUrlResultBO uploadBO = storageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO(storageKey));

        resourceMapper.insert(file);

        return new ResourceCreateVO(fileId, uploadBO.getUploadUrl(), uploadBO.getKey(), file.getStorageType());
    }

    public String getResourceReadUrl(Long fileId) {
        Map<Long, ResourceReadUrlGetResultBO> resourceMap = this.getResourceReadUrls(List.of(fileId));
        ResourceReadUrlGetResultBO resource = resourceMap.get(fileId);
        if (resource == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        return resource.getUrl();
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
        query.eq(Resource::getProcessStatus, FileProcessStatus.Activated);

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
     * 将业务对象资源更新为指定资源。
     * <p>
     * 新资源在同一调用中绑定并激活；与新资源不同的旧资源会被废弃。
     * {@code resourceId} 为 {@code null} 或非正数时表示清空业务对象资源。
     */
    public void bindBizResource(ResourceBindParamsBO params) {
        if (params.getBizId() == null) {
            return;
        }
        Long resourceId = params.getResourceId();
        if (isResourceId(resourceId)) {
            if (Objects.equals(resourceId, params.getReplacedResourceId())) {
                return;
            }
            Resource resource = loadOwnedResource(resourceId, params);
            ensureUploaded(resource, params);
            int updated = resourceMapper.update(null,
                    new LambdaUpdateWrapper<Resource>()
                            .eq(Resource::getId, resourceId)
                            .eq(Resource::getCreatorId, params.getCreatorId())
                            .eq(params.getOrgId() != null, Resource::getOrgId, params.getOrgId())
                            .eq(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                            .set(Resource::getBizId, params.getBizId())
                            .set(Resource::getProcessStatus, FileProcessStatus.Activated)
                            .set(Resource::getUpdatedAt, LocalDateTime.now()));
            if (updated == 0) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
            }
        }
        if (!Objects.equals(resourceId, params.getReplacedResourceId())) {
            deprecateReplacedResource(params.getReplacedResourceId(), params.getBizId(), params.getOrgId());
        }
    }

    private Resource loadOwnedResource(Long resourceId, ResourceBindParamsBO params) {
        Resource resource = resourceMapper.selectOne(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getCreatorId, params.getCreatorId())
                .eq(params.getOrgId() != null, Resource::getOrgId, params.getOrgId()));
        if (resource == null) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
        }
        return resource;
    }

    private void ensureUploaded(Resource resource, ResourceBindParamsBO params) {
        if (resource.getProcessStatus() == FileProcessStatus.Pending) {
            storageFactory.get(resource.getStorageType()).probe(
                    new ResourceProbeParamsBO(resource.getStorageKey(), resource.getFileSize(), resource.getFileMime()));
            int updated = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                    .eq(Resource::getId, resource.getId())
                    .eq(Resource::getCreatorId, params.getCreatorId())
                    .eq(params.getOrgId() != null, Resource::getOrgId, params.getOrgId())
                    .eq(Resource::getProcessStatus, FileProcessStatus.Pending)
                    .set(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                    .set(Resource::getUpdatedAt, LocalDateTime.now()));
            if (updated == 1) {
                return;
            }
            resource = loadOwnedResource(resource.getId(), params);
        }
        if (resource.getProcessStatus() != FileProcessStatus.Uploaded) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
        }
    }

    private void deprecateReplacedResource(Long resourceId, Long bizId, Long orgId) {
        if (!isResourceId(resourceId)) {
            return;
        }
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getBizId, bizId)
                .eq(orgId != null, Resource::getOrgId, orgId)
                .eq(Resource::getProcessStatus, FileProcessStatus.Activated)
                .set(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .set(Resource::getUpdatedAt, LocalDateTime.now(clock)));
    }

    private boolean isResourceId(Long resourceId) {
        return resourceId != null && resourceId > 0;
    }

    public Resource loadPending(String storageKey) {
        Resource resource = resourceMapper.selectOne(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getStorageKey, storageKey)
                .eq(Resource::getProcessStatus, FileProcessStatus.Pending)
                .last("LIMIT 1"));
        if (resource == null) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权上传或文件已完成上传");
        }
        return resource;
    }

    public Resource loadActivated(String storageKey) {
        Resource resource = resourceMapper.selectOne(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getStorageKey, storageKey)
                .eq(Resource::getProcessStatus, FileProcessStatus.Activated)
                .last("LIMIT 1"));
        if (resource == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        return resource;
    }

    private String buildStorageKey(String bizType, Long fileId, String ext) {
        return String.format("%s.%s", Paths.get(bizType, String.valueOf(fileId)), ext);
    }

    private boolean isImageBusiness(String bizType) {
        return "avatar".equals(bizType) || "cover".equals(bizType);
    }

}
