package online.longlian.app.service.app.impl.project;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectAssembler {

    private final ProjectTypeMapper projectTypeMapper;
    private final UserMapper userMapper;
    private final ResourceService resourceService;

    public List<ProjectListResultBO> assembleList(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> typeIds = projects.stream().map(Project::getTypeId).distinct().toList();
        Map<Long, String> typeNameMap = projectTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(ProjectType::getId, ProjectType::getName));

        List<Long> creatorIds = projects.stream().map(Project::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> coverFileIds = projects.stream()
                .map(Project::getCoverFileId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> coverUrlMap = coverFileIds.stream()
                .collect(Collectors.toMap(Function.identity(), resourceService::getResourceReadUrl));

        List<Long> avatarFileIds = userMap.values().stream()
                .map(User::getAvatarFileId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> avatarUrlMap = avatarFileIds.stream()
                .collect(Collectors.toMap(Function.identity(), resourceService::getResourceReadUrl));

        return projects.stream()
                .map(project -> {
                    User creator = userMap.get(project.getCreatorId());
                    String avatarUrl = null;
                    if (creator != null && creator.getAvatarFileId() != null) {
                        avatarUrl = avatarUrlMap.get(creator.getAvatarFileId());
                    }
                    return ProjectListResultBO.builder()
                            .id(project.getId())
                            .title(project.getTitle())
                            .description(project.getDescription())
                            .coverUrl(coverUrlMap.get(project.getCoverFileId()))
                            .projectType(typeNameMap.get(project.getTypeId()))
                            .projectStatus(project.getStatus())
                            .metadata(project.getMetadata())
                            .creatorAvatarUrl(avatarUrl)
                            .build();
                })
                .toList();
    }
}
