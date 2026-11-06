package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 把业务数据不再引用的已激活资源标为弃置。不删除存储，删除仍由资源清理任务按保留期处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDeprecationService {
    private static final int BATCH_SIZE = 100;

    private final ResourceMapper resourceMapper;

    /** 文件页删除任务提交文件。封面、头像或企划附件仍引用时返回 false。 */
    public boolean deprecateTaskSubmitFile(Long resourceId, Long taskInstanceId, Long orgId, LocalDateTime executeTime) {
        int updated = resourceMapper.deprecateTaskSubmitFile(resourceId, taskInstanceId, orgId, executeTime,
                FileProcessStatus.Activated.getCode(), FileProcessStatus.Deprecated.getCode());
        if (updated != 1) {
            return false;
        }
        log.info("资源弃置: id={}, bizType=task_submit, bizId={}, orgId={}, executeTime={}",
                resourceId, taskInstanceId, orgId, executeTime);
        return true;
    }

    public Set<Long> deprecatedIds(Collection<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Set.of();
        }
        return resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                        .select(Resource::getId)
                        .in(Resource::getId, resourceIds)
                        .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated))
                .stream()
                .map(Resource::getId)
                .collect(Collectors.toSet());
    }

    public void deprecateUnreferenced(LocalDateTime executeTime) {
        List<Resource> deprecated = new ArrayList<>();
        long lastId = 0L;
        while (true) {
            List<Resource> batch = resourceMapper.selectList(activatedBatch(lastId));
            if (batch.isEmpty()) {
                break;
            }
            for (Resource resource : batch) {
                lastId = resource.getId();
                int updated = resourceMapper.deprecateIfUnreferenced(resource.getId(), executeTime,
                        FileProcessStatus.Activated.getCode(), FileProcessStatus.Deprecated.getCode());
                if (updated == 1) {
                    deprecated.add(resource);
                }
            }
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        if (deprecated.isEmpty()) {
            log.info("资源弃置本轮没有弃置资源: executeTime={}", executeTime);
            return;
        }
        for (Resource resource : deprecated) {
            log.info("资源弃置: id={}, bizType={}, bizId={}, orgId={}, executeTime={}",
                    resource.getId(), resource.getBizType(), resource.getBizId(), resource.getOrgId(), executeTime);
        }
        log.info("资源弃置本轮完成: count={}, executeTime={}", deprecated.size(), executeTime);
    }

    private LambdaQueryWrapper<Resource> activatedBatch(long lastId) {
        // 分批扫描。LIMIT 无法用 Wrapper 的类型安全方法表达，和未绑定清理一样显式限制批量。
        return new LambdaQueryWrapper<Resource>()
                .select(Resource::getId, Resource::getBizType, Resource::getBizId, Resource::getOrgId)
                .eq(Resource::getProcessStatus, FileProcessStatus.Activated)
                .isNull(Resource::getStorageCleanedAt)
                .isNull(Resource::getDeletedAt)
                .gt(Resource::getId, lastId)
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE);
    }
}
