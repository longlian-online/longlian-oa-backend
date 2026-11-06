package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 把业务数据不再引用的已激活资源标为弃置。不删除存储，删除仍由资源清理任务按保留期处理。
 */
@Service
@RequiredArgsConstructor
public class ResourceDeprecationService {
    private static final int BATCH_SIZE = 100;

    private final ResourceMapper resourceMapper;

    public void deprecateUnreferenced(LocalDateTime executeTime) {
        long lastId = 0L;
        while (true) {
            List<Resource> batch = resourceMapper.selectList(activatedBatch(lastId));
            if (batch.isEmpty()) {
                return;
            }
            for (Resource resource : batch) {
                lastId = resource.getId();
                resourceMapper.deprecateIfUnreferenced(resource.getId(), executeTime,
                        FileProcessStatus.Activated.getCode(), FileProcessStatus.Deprecated.getCode());
            }
            if (batch.size() < BATCH_SIZE) {
                return;
            }
        }
    }

    private LambdaQueryWrapper<Resource> activatedBatch(long lastId) {
        // 分批扫描。LIMIT 无法用 Wrapper 的类型安全方法表达，和未绑定清理一样显式限制批量。
        return new LambdaQueryWrapper<Resource>()
                .select(Resource::getId)
                .eq(Resource::getProcessStatus, FileProcessStatus.Activated)
                .isNull(Resource::getStorageCleanedAt)
                .isNull(Resource::getDeletedAt)
                .gt(Resource::getId, lastId)
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE);
    }
}
