package online.longlian.app.service.orgadmin.impl.projecttype;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
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
public class ProjectTypeAssembler {

    private final UserMapper userMapper;

    public List<ProjectTypeListResultBO> assembleList(List<ProjectType> projectTypes) {
        if (projectTypes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> creatorIds = projectTypes.stream().map(ProjectType::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return projectTypes.stream()
                .map(projectType -> {
                    User creator = userMap.get(projectType.getCreatorId());
                    return ProjectTypeListResultBO.builder()
                            .id(projectType.getId())
                            .name(projectType.getName())
                            .status(projectType.getStatus())
                            .creatorId(projectType.getCreatorId())
                            .creatorNickname(creator != null ? creator.getNickname() : null)
                            .createdAt(projectType.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
