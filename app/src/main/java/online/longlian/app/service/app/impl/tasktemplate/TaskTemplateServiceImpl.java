package online.longlian.app.service.app.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionNodeVO;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;
import online.longlian.app.service.app.TaskTemplateService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final BaseTaskMapper baseTaskMapper;

    @Override
    public List<TaskTemplateOptionVO> listOptions(TaskTemplateOptionsParamsBO params) {
        List<TaskTemplate> orgTemplates = taskTemplateMapper.selectList(
                new LambdaQueryWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getOrgId, params.getOrgId())
                        .eq(TaskTemplate::getScope, TaskTemplateScope.ORGANIZATION)
                        .eq(TaskTemplate::getStatus, Status.ENABLED));

        List<TaskTemplate> personalTemplates = taskTemplateMapper.selectList(
                new LambdaQueryWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getCreatorId, params.getUserId())
                        .eq(TaskTemplate::getScope, TaskTemplateScope.PERSONAL)
                        .eq(TaskTemplate::getStatus, Status.ENABLED));

        List<TaskTemplate> allTemplates = new ArrayList<>();
        allTemplates.addAll(orgTemplates);
        allTemplates.addAll(personalTemplates);
        allTemplates.sort(Comparator.comparing(TaskTemplate::getCreatedAt).reversed());

        Map<Long, List<TaskTemplateNode>> nodesByTemplate = nodesByTemplate(allTemplates);
        Map<Long, BaseTask> baseTasks = baseTasks(nodesByTemplate);

        return allTemplates.stream()
                .map(template -> {
                    TaskTemplateOptionVO option = new TaskTemplateOptionVO();
                    option.setId(template.getId());
                    option.setName(template.getName());
                    option.setNodes(toNodes(nodesByTemplate.getOrDefault(template.getId(), List.of()), baseTasks));
                    return option;
                })
                .toList();
    }

    private Map<Long, List<TaskTemplateNode>> nodesByTemplate(List<TaskTemplate> templates) {
        if (templates.isEmpty()) {
            return Map.of();
        }
        List<Long> templateIds = templates.stream().map(TaskTemplate::getId).toList();
        List<TaskTemplateNode> nodes = taskTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<TaskTemplateNode>()
                        .in(TaskTemplateNode::getTaskTemplateId, templateIds)
                        .orderByAsc(TaskTemplateNode::getSort)
                        .orderByAsc(TaskTemplateNode::getParallelSort));
        Map<Long, List<TaskTemplateNode>> grouped = new HashMap<>();
        for (TaskTemplateNode node : nodes) {
            grouped.computeIfAbsent(node.getTaskTemplateId(), ignored -> new ArrayList<>()).add(node);
        }
        return grouped;
    }

    private Map<Long, BaseTask> baseTasks(Map<Long, List<TaskTemplateNode>> nodesByTemplate) {
        List<Long> baseTaskIds = nodesByTemplate.values().stream()
                .flatMap(List::stream)
                .map(TaskTemplateNode::getBaseTaskId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (baseTaskIds.isEmpty()) {
            return Map.of();
        }
        return baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                .collect(Collectors.toMap(BaseTask::getId, Function.identity()));
    }

    private List<TaskTemplateOptionNodeVO> toNodes(List<TaskTemplateNode> nodes, Map<Long, BaseTask> baseTasks) {
        List<TaskTemplateOptionNodeVO> result = new ArrayList<>();
        for (TaskTemplateNode node : nodes) {
            BaseTask baseTask = baseTasks.get(node.getBaseTaskId());
            TaskTemplateOptionNodeVO optionNode = new TaskTemplateOptionNodeVO();
            optionNode.setBaseTaskId(node.getBaseTaskId());
            optionNode.setBaseTaskName(baseTask == null ? null : baseTask.getName());
            optionNode.setBaseTaskStatus(baseTask == null ? null : baseTask.getStatus());
            optionNode.setSort(node.getSort());
            optionNode.setParallelSort(node.getParallelSort());
            result.add(optionNode);
        }
        return result;
    }
}
