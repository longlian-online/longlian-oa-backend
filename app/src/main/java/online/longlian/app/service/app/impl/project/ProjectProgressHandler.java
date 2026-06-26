package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectProgressHandler {

    private final ItemMapper itemMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ProjectWorkshopMapper projectWorkshopMapper;
    private final Clock clock;

    public ProjectProgressBO computeProgress(Long projectId) {
        List<Object> itemIdObjs = itemMapper.selectObjs(
                new LambdaQueryWrapper<Item>()
                        .select(Item::getId)
                        .eq(Item::getProjectId, projectId)
                        .isNull(Item::getDeletedAt));

        if (itemIdObjs.isEmpty()) {
            return ProjectProgressBO.builder()
                    .progressPercent(0)
                    .claimedTaskCount(0)
                    .pendingTaskCount(0)
                    .build();
        }

        List<Long> itemIds = itemIdObjs.stream().map(id -> (Long) id).toList();
        int totalItems = itemIds.size();

        long publishedCount = itemMapper.selectCount(
                new LambdaQueryWrapper<Item>()
                        .in(Item::getId, itemIds)
                        .eq(Item::getStatus, ItemStatus.PUBLISHED));
        int progressPercent = (int) (publishedCount * 100 / totalItems);

        long claimedCount = taskInstanceMapper.selectCount(
                new LambdaQueryWrapper<TaskInstance>()
                        .in(TaskInstance::getItemId, itemIds)
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .isNull(TaskInstance::getDeletedAt));
        long pendingCount = taskInstanceMapper.selectCount(
                new LambdaQueryWrapper<TaskInstance>()
                        .in(TaskInstance::getItemId, itemIds)
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .isNull(TaskInstance::getDeletedAt));

        return ProjectProgressBO.builder()
                .progressPercent(progressPercent)
                .claimedTaskCount((int) claimedCount)
                .pendingTaskCount((int) pendingCount)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void addToWorkshop(ProjectWorkshopAddParamsBO params) {
        boolean exists = projectWorkshopMapper.selectCount(
                new LambdaQueryWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, params.getProjectId())
                        .eq(ProjectWorkshop::getUserId, params.getUserId())
                        .isNull(ProjectWorkshop::getDeletedAt)
        ) > 0;
        if (exists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        ProjectWorkshop workshop = ProjectWorkshop.builder()
                .projectId(params.getProjectId())
                .userId(params.getUserId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        projectWorkshopMapper.insert(workshop);
    }
}
