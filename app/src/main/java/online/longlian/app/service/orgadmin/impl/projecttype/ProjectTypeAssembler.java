package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
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
public class ProjectTypeAssembler {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    public List<ProjectTypeListResultBO> assembleList(List<ProjectType> projectTypes, Long orgId) {
        if (projectTypes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> creatorIds = projectTypes.stream().map(ProjectType::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> typeIds = projectTypes.stream().map(ProjectType::getId).toList();
        // 只按 type_id 分组计数，不拉整行；条件与 deleteProjectType 引用检查一致（orgId + typeId，逻辑删除自动排除）
        List<Map<String, Object>> countRows = projectMapper.selectMaps(
                new QueryWrapper<Project>()
                        .select("type_id", "COUNT(*) AS ref_count")
                        .eq("org_id", orgId)
                        .in("type_id", typeIds)
                        .groupBy("type_id")
        );
        Map<Long, Long> refCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("type_id")).longValue(),
                        row -> ((Number) row.get("ref_count")).longValue()));

        return projectTypes.stream()
                .map(projectType -> {
                    User creator = userMap.get(projectType.getCreatorId());
                    return ProjectTypeListResultBO.builder()
                            .id(projectType.getId())
                            .name(projectType.getName())
                            .priority(projectType.getPriority())
                            .refCount(refCountMap.getOrDefault(projectType.getId(), 0L).intValue())
                            .status(projectType.getStatus())
                            .creatorId(projectType.getCreatorId())
                            .creatorNickname(creator != null ? creator.getNickname() : null)
                            .createdAt(projectType.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
