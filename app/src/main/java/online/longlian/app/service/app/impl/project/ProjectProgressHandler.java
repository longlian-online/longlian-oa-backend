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
                        .eq(Item::getProjectId, projectId));

        if (itemIdObjs.isEmpty()) {
            return ProjectProgressBO.builder()
                    .progressPercent(0)
                    .claimedTaskCount(0)
                    .pendingTaskCount(0)
                    .build();
        }

        List<Long> itemIds = itemIdObjs.stream().map(id -> (Long) id).toList();

        long totalCount = countTaskInstances(itemIds, null);
        long completedCount = countTaskInstances(itemIds, TaskInstanceStatus.COMPLETED);
        long claimedCount = countTaskInstances(itemIds, TaskInstanceStatus.CLAIMED);
        long pendingCount = countTaskInstances(itemIds, TaskInstanceStatus.PENDING);
        int progressPercent = totalCount == 0 ? 0 : (int) (completedCount * 100 / totalCount);

        return ProjectProgressBO.builder()
                .progressPercent(progressPercent)
                .claimedTaskCount((int) claimedCount)
                .pendingTaskCount((int) pendingCount)
                .build();
    }

    private long countTaskInstances(List<Long> itemIds, TaskInstanceStatus status) {
        LambdaQueryWrapper<TaskInstance> query = new LambdaQueryWrapper<TaskInstance>()
                .in(TaskInstance::getItemId, itemIds);
        if (status != null) {
            query.eq(TaskInstance::getStatus, status);
        }
        return taskInstanceMapper.selectCount(query);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addToWorkshop(ProjectWorkshopAddParamsBO params) {
        boolean exists = projectWorkshopMapper.selectCount(
                new LambdaQueryWrapper<ProjectWorkshop>()
                        .eq(ProjectWorkshop::getProjectId, params.getProjectId())
                        .eq(ProjectWorkshop::getUserId, params.getUserId())
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
