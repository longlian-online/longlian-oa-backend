package online.longlian.app.service.app.impl.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.*;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.service.app.impl.taskinstance.SlotLabels;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知联合读视图直接批量读取跨领域 Mapper，避免逐条详情 N+1，
 * 也避免通过任务写服务形成 TaskInstanceService → InboxNotificationService → TaskInstanceService 循环。
 */
@Component
@RequiredArgsConstructor
public class NotificationSourceResolver {
    private final TaskInstanceMapper taskInstanceMapper;
    private final ItemMapper itemMapper;
    private final ProjectMapper projectMapper;

    public Map<Long, NotificationResolvedSourceBO> resolve(NotificationSourceResolveParamsBO params) {
        if (params.getSources().isEmpty()) {
            return Map.of();
        }
        Map<InboxNotificationKind, List<NotificationSourceRefBO>> groups = params.getSources().stream()
                .collect(Collectors.groupingBy(NotificationSourceRefBO::getKind,
                        () -> new EnumMap<>(InboxNotificationKind.class), Collectors.toList()));
        List<Long> taskIds = sourceIds(groups.getOrDefault(InboxNotificationKind.TASK_READY, List.of()));
        Map<Long, TaskInstance> tasks = taskIds.isEmpty() ? Map.of() : taskInstanceMapper.selectBatchIds(taskIds)
                .stream().collect(Collectors.toMap(TaskInstance::getId, Function.identity()));
        Set<Long> itemIds = new HashSet<>(sourceIds(groups.getOrDefault(InboxNotificationKind.ITEM_COMPLETED, List.of())));
        tasks.values().forEach(task -> itemIds.add(task.getItemId()));
        Map<Long, Item> items = itemIds.isEmpty() ? Map.of() : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        List<Long> projectIds = items.values().stream().map(Item::getProjectId).distinct().toList();
        Map<Long, Project> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectList(
                new LambdaQueryWrapper<Project>().in(Project::getId, projectIds)
                        .eq(Project::getOrgId, params.getOrgId()).eq(Project::getResourceStatus, Status.ENABLED))
                .stream().collect(Collectors.toMap(Project::getId, Function.identity()));
        List<Long> taskItemIds = tasks.values().stream()
                .filter(task -> visibleTask(task, items, projects))
                .map(TaskInstance::getItemId).distinct().toList();
        Map<Long, List<ItemStageNodeStatusBO>> nodes = taskItemIds.isEmpty() ? Map.of()
                : taskInstanceMapper.selectItemStageNodeStatuses(taskItemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));
        Map<Long, NotificationResolvedSourceBO> results = new HashMap<>();
        for (var group : groups.entrySet()) {
            Map<Long, NotificationResolvedSourceBO> sources = switch (group.getKey()) {
                case TASK_READY -> resolveTaskSources(sourceIds(group.getValue()), tasks, items, projects, nodes);
                case ITEM_COMPLETED -> resolveItemSources(sourceIds(group.getValue()), items, projects);
            };
            for (NotificationSourceRefBO ref : group.getValue()) {
                results.put(ref.getNotificationId(), sources.get(ref.getSourceId()));
            }
        }
        return results;
    }

    private List<Long> sourceIds(List<NotificationSourceRefBO> refs) {
        return refs.stream().map(NotificationSourceRefBO::getSourceId).distinct().toList();
    }

    private boolean visibleTask(TaskInstance task, Map<Long, Item> items, Map<Long, Project> projects) {
        Item item = items.get(task.getItemId());
        return item != null && Objects.equals(task.getProjectId(), item.getProjectId())
                && projects.containsKey(item.getProjectId());
    }

    private Map<Long, NotificationResolvedSourceBO> resolveTaskSources(List<Long> ids, Map<Long, TaskInstance> tasks,
            Map<Long, Item> items, Map<Long, Project> projects, Map<Long, List<ItemStageNodeStatusBO>> nodes) {
        Map<Long, NotificationResolvedSourceBO> sources = new HashMap<>();
        for (Long id : ids) {
            TaskInstance task = tasks.get(id);
            List<ItemStageNodeStatusBO> itemNodes = task == null ? List.of() : nodes.getOrDefault(task.getItemId(), List.of());
            ItemStageNodeStatusBO node = task == null ? null : itemNodes.stream()
                    .filter(candidate -> task.getItemTaskNodeId().equals(candidate.getNodeId())).findFirst().orElse(null);
            if (task == null || !visibleTask(task, items, projects) || node == null) {
                sources.put(id, unavailable(InboxNotificationKind.TASK_READY));
                continue;
            }
            Item item = items.get(task.getItemId());
            Project project = projects.get(item.getProjectId());
            String slot = formatTaskSlotLabel(node.getNodeName(), SlotLabels.of(node.getNodeId(), itemNodes));
            sources.put(id, resolved(InboxNotificationKind.TASK_READY,
                    "「" + project.getTitle() + " · " + item.getTitle() + "」的前置步骤已完成，你接取的「" + slot + "」可以开始了。",
                    "/dashboard/planning/" + project.getId() + "/items/" + item.getId() + "?taskInstanceId=" + id, project));
        }
        return sources;
    }

    private Map<Long, NotificationResolvedSourceBO> resolveItemSources(List<Long> ids, Map<Long, Item> items,
                                                                       Map<Long, Project> projects) {
        Map<Long, NotificationResolvedSourceBO> sources = new HashMap<>();
        for (Long id : ids) {
            Item item = items.get(id);
            Project project = item == null ? null : projects.get(item.getProjectId());
            if (project == null) {
                sources.put(id, unavailable(InboxNotificationKind.ITEM_COMPLETED));
                continue;
            }
            sources.put(id, resolved(InboxNotificationKind.ITEM_COMPLETED,
                    "「" + project.getTitle() + " · " + item.getTitle() + "」的全部任务已完成。",
                    "/dashboard/planning/" + project.getId() + "/items/" + id, project));
        }
        return sources;
    }

    private String formatTaskSlotLabel(String name, SlotLabels.Position position) {
        String step = position.serialStep() == null ? "" : "（第 " + position.serialStep() + " 步）";
        String parallel = position.slotTotal() > 1 ? " · " + position.slotIndex() + "/" + position.slotTotal() : "";
        return name + step + parallel;
    }

    private NotificationResolvedSourceBO unavailable(InboxNotificationKind kind) {
        return new NotificationResolvedSourceBO(new NotificationContentResultBO(kind.getDesc(),
                "通知关联内容已不可访问", null, null), List.of());
    }

    private NotificationResolvedSourceBO resolved(InboxNotificationKind kind, String message, String path, Project project) {
        List<Long> images = new ArrayList<>(2);
        if (project.getThumbnailFileId() != null && project.getThumbnailFileId() > 0) {
            images.add(project.getThumbnailFileId());
        }
        if (project.getCoverFileId() != null && project.getCoverFileId() > 0 && !images.contains(project.getCoverFileId())) {
            images.add(project.getCoverFileId());
        }
        return new NotificationResolvedSourceBO(new NotificationContentResultBO(kind.getDesc(), message, path, null), images);
    }
}
