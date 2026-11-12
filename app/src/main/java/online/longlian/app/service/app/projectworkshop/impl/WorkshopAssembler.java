package online.longlian.app.service.app.projectworkshop.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.bo.app.projectworkshop.ProjectLatestSubmissionBO;
import online.longlian.app.pojo.bo.app.projectworkshop.WorkshopProjectInfoResultBO;
import online.longlian.app.pojo.bo.app.projectworkshop.WorkshopTaskTemplateNodeResultBO;
import online.longlian.app.pojo.bo.app.projectworkshop.WorkshopTaskTemplateResultBO;
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

    /** 工坊卡片缩略图按 EdgeOne 图片即时处理参数缩放到 144x144。 */
    private static final String THUMBNAIL_RESIZE_PARAM = "eo-img.resize=w/144/h/144";

    private final UserMapper userMapper;
    private final BaseTaskMapper baseTaskMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final ResourceService resourceService;
    private final TaskSubmissionMapper taskSubmissionMapper;

    public List<WorkshopProjectInfoResultBO> assembleProjectList(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProjectLatestSubmissionBO> latestSubmissionMap = taskSubmissionMapper.selectLatestForProjects(
                        projects.stream().map(Project::getId).toList()).stream()
                .collect(Collectors.toMap(ProjectLatestSubmissionBO::getId, Function.identity()));

        List<Long> coverFileIds = projects.stream()
                .map(Project::getCoverFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> coverUrlMap = coverFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(coverFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        List<Long> thumbnailFileIds = projects.stream()
                .map(Project::getThumbnailFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> thumbnailUrlMap = thumbnailFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(thumbnailFileIds).entrySet().stream()
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
                    WorkshopProjectInfoResultBO projectInfoResultBO = new WorkshopProjectInfoResultBO();
                    projectInfoResultBO.setId(project.getId());
                    projectInfoResultBO.setTitle(project.getTitle());
                    projectInfoResultBO.setCoverUrl(coverUrlMap.get(project.getCoverFileId()));
                    projectInfoResultBO.setThumbnailUrl(
                            resizeThumbnailUrl(thumbnailUrlMap.get(project.getThumbnailFileId())));
                    if (creator != null && creator.getAvatarFileId() != null) {
                        projectInfoResultBO.setCreatorAvatarUrl(avatarUrlMap.get(creator.getAvatarFileId()));
                    }
                    ProjectLatestSubmissionBO latestSubmission = latestSubmissionMap.get(project.getId());
                    if (latestSubmission != null) {
                        projectInfoResultBO.setLastSubmitterUsername(latestSubmission.getLastSubmitterUsername());
                        projectInfoResultBO.setLastSubmitterAt(latestSubmission.getLastSubmitterAt());
                    }
                    return projectInfoResultBO;
                })
                .toList();
    }

    private static String resizeThumbnailUrl(String url) {
        if (url == null) {
            return null;
        }
        return url + (url.contains("?") ? "&" : "?") + THUMBNAIL_RESIZE_PARAM;
    }

    public List<WorkshopTaskTemplateResultBO> assembleTemplateList(List<TaskTemplate> templates, Long currentUserId) {
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



        return templates.stream()
                .map(template -> {
                    WorkshopTaskTemplateResultBO templateResultBO = new WorkshopTaskTemplateResultBO();
                    templateResultBO.setId(template.getId());
                    templateResultBO.setName(template.getName());
                    templateResultBO.setDescription(template.getDescription());
                    templateResultBO.setScope(template.getScope());
                    templateResultBO.setIsMine(template.getCreatorId().equals(currentUserId));

                    List<TaskTemplateNode> nodes = nodeMap.getOrDefault(template.getId(), Collections.emptyList());
                    templateResultBO.setTaskCount(nodes.size());

                    List<WorkshopTaskTemplateNodeResultBO> nodeResultBOs = nodes.stream()
                            .map(node -> toNodeResultBO(node, baseTaskMap))
                            .toList();
                    templateResultBO.setNodes(nodeResultBOs);
                    return templateResultBO;
                })
                .toList();
    }

    private WorkshopTaskTemplateNodeResultBO toNodeResultBO(TaskTemplateNode node, Map<Long, BaseTask> baseTaskMap) {
        WorkshopTaskTemplateNodeResultBO resultBO = new WorkshopTaskTemplateNodeResultBO();
        resultBO.setBaseTaskId(node.getBaseTaskId());
        resultBO.setSort(node.getSort());
        resultBO.setParallelSort(node.getParallelSort());

        BaseTask baseTask = baseTaskMap.get(node.getBaseTaskId());
        if (baseTask != null) {
            resultBO.setBaseTaskName(baseTask.getName());
            resultBO.setBaseTaskIcon(baseTask.getIcon());
        }
        return resultBO;
    }
}
