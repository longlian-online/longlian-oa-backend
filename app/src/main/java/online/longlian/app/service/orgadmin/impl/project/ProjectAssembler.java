package online.longlian.app.service.orgadmin.impl.project;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectAssembler {

    private final ProjectTypeMapper projectTypeMapper;
    private final UserMapper userMapper;

    public List<ProjectAdminListResultBO> assembleAdminList(List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> typeIds = projects.stream().map(Project::getTypeId).distinct().toList();
        Map<Long, ProjectType> typeMap = projectTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(ProjectType::getId, Function.identity()));

        List<Long> creatorIds = projects.stream().map(Project::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return projects.stream()
                .map(project -> {
                    ProjectType projectType = typeMap.get(project.getTypeId());
                    User creator = userMap.get(project.getCreatorId());
                    return ProjectAdminListResultBO.builder()
                            .id(project.getId())
                            .title(project.getTitle())
                            .typeId(project.getTypeId())
                            .typeName(projectType != null ? projectType.getName() : null)
                            .status(project.getStatus())
                            .creatorId(project.getCreatorId())
                            .creatorNickname(creator != null ? creator.getNickname() : null)
                            .createdAt(project.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
