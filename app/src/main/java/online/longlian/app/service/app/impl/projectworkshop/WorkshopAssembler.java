package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateNodeVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkshopAssembler {

    private final UserMapper userMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final ResourceService resourceService;

    public List<WorkshopProjectInfoVO> assembleProjectList(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> coverFileIds = projects.stream()
                .map(Project::getCoverFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> coverUrlMap = coverFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(coverFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        List<Long> creatorIds = projects.stream().map(Project::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> avatarFileIds = userMap.values().stream()
                .map(User::getAvatarFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> avatarUrlMap = avatarFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(avatarFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        return projects.stream()
                .map(project -> {
                    User creator = userMap.get(project.getCreatorId());
                    WorkshopProjectInfoVO workshopProjectInfoVO = new WorkshopProjectInfoVO();
                    workshopProjectInfoVO.setId(project.getId());
                    workshopProjectInfoVO.setTitle(project.getTitle());
                    workshopProjectInfoVO.setCoverUrl(coverUrlMap.get(project.getCoverFileId()));
                    if (creator != null && creator.getAvatarFileId() != null) {
                        workshopProjectInfoVO.setCreatorAvatarUrl(avatarUrlMap.get(creator.getAvatarFileId()));
                    }
                    return workshopProjectInfoVO;
                })
                .toList();
    }

    public List<WorkshopTaskTemplateVO> assembleTemplateList(List<TaskTemplate> templates, Long currentUserId) {
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> templateIds = templates.stream().map(TaskTemplate::getId).toList();
        Map<Long, List<TaskTemplateNode>> nodeMap = taskTemplateNodeMapper.selectList(
                        new LambdaQueryWrapper<TaskTemplateNode>()
                                .in(TaskTemplateNode::getTaskTemplateId, templateIds)
                                .orderByAsc(TaskTemplateNode::getSort)
                                .orderByAsc(TaskTemplateNode::getParallelSort))
                .stream()
                .collect(Collectors.groupingBy(TaskTemplateNode::getTaskTemplateId));

        List<Long> baseTaskIds = nodeMap.values().stream()
                .flatMap(List::stream)
                .map(TaskTemplateNode::getBaseTaskId)
                .distinct()
                .toList();
        Map<Long, BaseTask> baseTaskMap = baseTaskIds.isEmpty()
                ? Collections.emptyMap()
                : baseTaskMapper.selectBatchIds(baseTaskIds).stream()
                        .collect(Collectors.toMap(BaseTask::getId, Function.identity()));

        List<Long> iconFileIds = baseTaskMap.values().stream()
                .map(BaseTask::getIconFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> iconUrlMap = iconFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(iconFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        return templates.stream()
                .map(template -> {
                    WorkshopTaskTemplateVO workshopTaskTemplateVO = new WorkshopTaskTemplateVO();
                    workshopTaskTemplateVO.setId(template.getId());
                    workshopTaskTemplateVO.setName(template.getName());
                    workshopTaskTemplateVO.setDescription(template.getDescription());
                    workshopTaskTemplateVO.setScope(template.getScope());
                    workshopTaskTemplateVO.setIsMine(template.getCreatorId().equals(currentUserId));

                    List<TaskTemplateNode> nodes = nodeMap.getOrDefault(template.getId(), Collections.emptyList());
                    workshopTaskTemplateVO.setTaskCount(nodes.size());

                    List<WorkshopTaskTemplateNodeVO> nodeVOs = nodes.stream()
                            .map(node -> toNodeVO(node, baseTaskMap, iconUrlMap))
                            .toList();
                    workshopTaskTemplateVO.setNodes(nodeVOs);
                    return workshopTaskTemplateVO;
                })
                .toList();
    }

    private WorkshopTaskTemplateNodeVO toNodeVO(TaskTemplateNode node, Map<Long, BaseTask> baseTaskMap, Map<Long, String> iconUrlMap) {
        WorkshopTaskTemplateNodeVO vo = new WorkshopTaskTemplateNodeVO();
        vo.setBaseTaskId(node.getBaseTaskId());
        vo.setSort(node.getSort());
        vo.setParallelSort(node.getParallelSort());

        BaseTask baseTask = baseTaskMap.get(node.getBaseTaskId());
        if (baseTask != null) {
            vo.setBaseTaskName(baseTask.getName());
            if (baseTask.getIconFileId() != null) {
                vo.setBaseTaskIconUrl(iconUrlMap.get(baseTask.getIconFileId()));
            }
        }
        return vo;
    }
}
