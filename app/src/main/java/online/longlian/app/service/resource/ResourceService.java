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
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
    private final CdnUrlSigner cdnUrlSigner;
    private final LocalFileUrlSigner localFileUrlSigner;

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
        long cdnTimestamp = isCdnEnabled() && !resources.isEmpty() ? currentCdnTimestamp() : 0;

        Map<String, Long> resourceIdKeyMap = resources.stream().collect(Collectors.toMap(Resource::getStorageKey, Resource::getId));

        Stream<ResourceReadUrlGetResultBO> resourceReadUrlResultStream = resources.stream().map(resource ->
                new ResourceReadUrlGetResultBO(
                        getCdnReadUrl(resource, cdnTimestamp),
                        resource.getOrgId(),
                        resource.getStorageKey()
                )
        );
        return resourceReadUrlResultStream.collect(Collectors.toMap((org) -> resourceIdKeyMap.get(org.getKey()), org -> org));
    }

    private String getCdnReadUrl(Resource resource, long cdnTimestamp) {
        if (!isCdnEnabled()) {
            return storageFactory.get(resource.getStorageType()).getResourceReadUrl(resource.getStorageKey());
        }
        if (resource.getStorageType() != StorageType.LOCAL) {
            return cdnUrlSigner.sign(resource.getStorageKey(), cdnTimestamp);
        }
        LocalFileReadParamsBO signed = localFileUrlSigner.sign(resource.getStorageKey(),
                Math.addExact(cdnTimestamp, storageProperties.getCdn().getAuthTtlSeconds()));
        String query = "key=" + UriUtils.encodeQueryParam(signed.key(), StandardCharsets.UTF_8)
                + "&expires=" + signed.expires()
                + "&signature=" + signed.signature();
        return cdnUrlSigner.signPath("/common/file/local", query, cdnTimestamp);
    }

    private long currentCdnTimestamp() {
        StorageProperties.CdnConfig cdn = storageProperties.getCdn();
        long authTtlSeconds = cdn.getAuthTtlSeconds();
        int urlReusePercent = cdn.getUrlReusePercent();
        long reuseWindowSeconds = authTtlSeconds / 100 * urlReusePercent
                + ((authTtlSeconds % 100) * urlReusePercent) / 100;
        if (authTtlSeconds <= 1 || urlReusePercent <= 0 || urlReusePercent >= 100
                || reuseWindowSeconds <= 0 || reuseWindowSeconds >= authTtlSeconds) {
            throw new IllegalStateException("CDN 鉴权有效期和链接复用比例配置无效");
        }
        return Math.multiplyExact(Math.floorDiv(clock.instant().getEpochSecond(), reuseWindowSeconds), reuseWindowSeconds);
    }

    private boolean isCdnEnabled() {
        return storageProperties.getCdn() != null && storageProperties.getCdn().isEnabled();
    }

    /** Activated resources for one business object. IDs that do not match are omitted and are not signed. */
    public Map<Long, ActivatedResource> findActivated(String bizType, Long bizId, Long orgId, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Resource> resources = loadResources(resourceIds);
        Map<Long, ActivatedResource> matched = new HashMap<>();
        for (Long resourceId : resourceIds) {
            Resource resource = resources.get(resourceId);
            if (resource == null || !Objects.equals(resource.getOrgId(), orgId)
                    || !Objects.equals(resource.getBizType(), bizType)
                    || !Objects.equals(resource.getBizId(), bizId)
                    || resource.getProcessStatus() != FileProcessStatus.Activated) {
                continue;
            }
            matched.put(resourceId, new ActivatedResource(
                    resource.getId(), resource.getFileName(), resource.getFileSize(), resource.getFileMime(),
                    resource.getStorageType(), resource.getStorageKey(), resource.getOrgId()));
        }
        return matched;
    }

    public boolean cdnEnabled() {
        return isCdnEnabled();
    }

    /** Sign already authorized activated resources. The caller decides whether CDN is mandatory. */
    public Map<Long, ActivatedResourceRead> signActivated(Collection<ActivatedResource> resources) {
        if (resources.isEmpty()) {
            return Map.of();
        }
        if (!isCdnEnabled()) {
            throw new IllegalStateException("签名读取必须启用 CDN");
        }
        long timestamp = currentCdnTimestamp();
        long expiresAt = Math.addExact(timestamp, storageProperties.getCdn().getAuthTtlSeconds());
        Map<Long, ActivatedResourceRead> reads = new HashMap<>();
        for (ActivatedResource item : resources) {
            Resource resource = Resource.builder()
                    .storageType(item.storageType())
                    .storageKey(item.storageKey())
                    .build();
            reads.put(item.id(), new ActivatedResourceRead(
                    item.id(), item.fileName(), item.fileSize(), item.fileMime(),
                    getCdnReadUrl(resource, timestamp), expiresAt));
        }
        return reads;
    }

    /**
     * 将业务对象资源更新为指定资源。
     * <p>
     * {@code reuseBound} 为 false 时，新资源在同一调用中绑定并激活，与新资源不同的旧资源会被废弃。
     * {@code resourceId} 为 {@code null} 或非正数时表示清空业务对象资源。
     * {@code reuseBound} 为 true 时，可一次绑定多份未绑定上传，并复用已激活且业务 ID 相同的资源，不废弃其他资源。
     */
    public void bindBizResource(ResourceBindParamsBO params) {
        if (params.getBizId() == null) {
            return;
        }
        List<Long> resourceIds = bindResourceIds(params);
        boolean reuseBound = Boolean.TRUE.equals(params.getReuseBound());
        if (!reuseBound && resourceIds.size() == 1
                && Objects.equals(resourceIds.get(0), params.getReplacedResourceId())) {
            return;
        }
        if (!resourceIds.isEmpty()) {
            if (params.getBizType() == null) {
                throw unauthorized();
            }
            Map<Long, Resource> resources = loadResources(resourceIds);
            for (Long resourceId : resourceIds) {
                validateBind(resources.get(resourceId), params, reuseBound);
            }
            for (Long resourceId : resourceIds) {
                activateBound(resources.get(resourceId), params, reuseBound);
            }
        }
        if (!reuseBound) {
            Long currentId = resourceIds.size() == 1 ? resourceIds.get(0) : null;
            if (!Objects.equals(currentId, params.getReplacedResourceId())) {
                deprecateReplacedResource(params.getReplacedResourceId(), params.getBizId(), params.getOrgId());
            }
        }
    }

    private List<Long> bindResourceIds(ResourceBindParamsBO params) {
        if (params.getResourceIds() != null) {
            List<Long> resourceIds = new ArrayList<>();
            for (Long resourceId : params.getResourceIds()) {
                if (isResourceId(resourceId)) {
                    resourceIds.add(resourceId);
                }
            }
            return resourceIds;
        }
        return isResourceId(params.getResourceId()) ? List.of(params.getResourceId()) : List.of();
    }

    private void validateBind(Resource resource, ResourceBindParamsBO params, boolean reuseBound) {
        if (reuseBound) {
            if (resource == null || !Objects.equals(resource.getOrgId(), params.getOrgId())
                    || !Objects.equals(resource.getCreatorId(), params.getCreatorId())
                    || !Objects.equals(resource.getBizType(), params.getBizType())) {
                throw unauthorized();
            }
            boolean reusable = resource.getProcessStatus() == FileProcessStatus.Activated
                    && Objects.equals(resource.getBizId(), params.getBizId());
            boolean unboundUpload = Objects.equals(resource.getBizId(), 0L)
                    && (resource.getProcessStatus() == FileProcessStatus.Pending
                    || resource.getProcessStatus() == FileProcessStatus.Uploaded);
            if (!reusable && !unboundUpload) {
                throw unauthorized();
            }
            return;
        }
        if (resource == null || !Objects.equals(resource.getCreatorId(), params.getCreatorId())
                || (params.getOrgId() != null && !Objects.equals(resource.getOrgId(), params.getOrgId()))
                || !Objects.equals(resource.getBizType(), params.getBizType())) {
            throw unauthorized();
        }
    }

    private void activateBound(Resource resource, ResourceBindParamsBO params, boolean reuseBound) {
        if (reuseBound) {
            if (resource.getProcessStatus() == FileProcessStatus.Activated) {
                return;
            }
            if (resource.getProcessStatus() == FileProcessStatus.Pending) {
                storageFactory.get(resource.getStorageType()).probe(new ResourceProbeParamsBO(
                        resource.getStorageKey(), resource.getFileSize(), resource.getFileMime()));
                int uploaded = resourceMapper.update(null, unboundUpdate(resource.getId(), params)
                        .eq(Resource::getProcessStatus, FileProcessStatus.Pending)
                        .set(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                        .set(Resource::getUpdatedAt, LocalDateTime.now(clock)));
                if (uploaded != 1) {
                    throw unauthorized();
                }
            }
            int activated = resourceMapper.update(null, unboundUpdate(resource.getId(), params)
                    .eq(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                    .set(Resource::getBizId, params.getBizId())
                    .set(Resource::getProcessStatus, FileProcessStatus.Activated)
                    .set(Resource::getUpdatedAt, LocalDateTime.now(clock)));
            if (activated != 1) {
                throw unauthorized();
            }
            return;
        }
        ensureUploaded(resource, params);
        int updated = resourceMapper.update(null, ownedUpdate(resource.getId(), params)
                .eq(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                .set(Resource::getBizId, params.getBizId())
                .set(Resource::getProcessStatus, FileProcessStatus.Activated)
                .set(Resource::getUpdatedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw unauthorized();
        }
    }

    private LambdaUpdateWrapper<Resource> unboundUpdate(Long resourceId, ResourceBindParamsBO params) {
        return new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getOrgId, params.getOrgId())
                .eq(Resource::getCreatorId, params.getCreatorId())
                .eq(Resource::getBizType, params.getBizType())
                .eq(Resource::getBizId, 0L);
    }

    private LambdaUpdateWrapper<Resource> ownedUpdate(Long resourceId, ResourceBindParamsBO params) {
        return new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getCreatorId, params.getCreatorId())
                .eq(params.getOrgId() != null, Resource::getOrgId, params.getOrgId())
                .eq(Resource::getBizType, params.getBizType());
    }

    private Resource loadOwnedResource(Long resourceId, ResourceBindParamsBO params) {
        Resource resource = resourceMapper.selectOne(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getCreatorId, params.getCreatorId())
                .eq(params.getOrgId() != null, Resource::getOrgId, params.getOrgId())
                .eq(Resource::getBizType, params.getBizType()));
        if (resource == null) {
            throw unauthorized();
        }
        return resource;
    }

    private void ensureUploaded(Resource resource, ResourceBindParamsBO params) {
        if (resource.getProcessStatus() == FileProcessStatus.Pending) {
            storageFactory.get(resource.getStorageType()).probe(
                    new ResourceProbeParamsBO(resource.getStorageKey(), resource.getFileSize(), resource.getFileMime()));
            int updated = resourceMapper.update(null, ownedUpdate(resource.getId(), params)
                    .eq(Resource::getProcessStatus, FileProcessStatus.Pending)
                    .set(Resource::getProcessStatus, FileProcessStatus.Uploaded)
                    .set(Resource::getUpdatedAt, LocalDateTime.now()));
            if (updated == 1) {
                return;
            }
            resource = loadOwnedResource(resource.getId(), params);
        }
        if (resource.getProcessStatus() != FileProcessStatus.Uploaded) {
            throw unauthorized();
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

    private Map<Long, Resource> loadResources(List<Long> resourceIds) {
        return resourceMapper.selectList(lambdaQuery(Resource.class).in(Resource::getId, resourceIds))
                .stream().collect(Collectors.toMap(Resource::getId, resource -> resource));
    }

    private boolean isResourceId(Long resourceId) {
        return resourceId != null && resourceId > 0;
    }

    private AppException unauthorized() {
        return new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
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
