package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ProjectProgressBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.common.enumeration.ItemStatus;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectProgressHandler {

    private final ItemMapper itemMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    public ProjectProgressBO computeProgress(Long projectId) {
        List<Item> items = itemMapper.selectList(
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getProjectId, projectId)
                        .isNull(Item::getDeletedAt));
        if (items.isEmpty()) {
            return ProjectProgressBO.builder()
                    .progressPercent(0)
                    .claimedTaskCount(0)
                    .pendingTaskCount(0)
                    .build();
        }

        int totalItems = items.size();
        long publishedCount = items.stream()
                .filter(i -> i.getStatus() == ItemStatus.PUBLISHED)
                .count();
        int progressPercent = (int) (publishedCount * 100 / totalItems);

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<TaskInstanceStatus, Long> statusCountMap = taskInstanceMapper.selectList(
                        new LambdaQueryWrapper<TaskInstance>()
                                .in(TaskInstance::getItemId, itemIds)
                                .isNull(TaskInstance::getDeletedAt))
                .stream()
                .collect(Collectors.groupingBy(TaskInstance::getStatus, Collectors.counting()));

        return ProjectProgressBO.builder()
                .progressPercent(progressPercent)
                .claimedTaskCount(statusCountMap.getOrDefault(TaskInstanceStatus.CLAIMED, 0L).intValue())
                .pendingTaskCount(statusCountMap.getOrDefault(TaskInstanceStatus.PENDING, 0L).intValue())
                .build();
    }
}
