package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkshopProjectHandler {

    private final ProjectTypeMapper projectTypeMapper;

    public List<Project> filterProjects(List<Long> orderedProjectIds,
                                        Map<Long, Project> projectMap,
                                        Long orgId,
                                        String projectTypeName,
                                        String keyword,
                                        Long userId,
                                        Boolean isMyCreated) {
        Long resolvedTypeId = resolveTypeId(orgId, projectTypeName);

        return orderedProjectIds.stream()
                .map(projectMap::get)
                .filter(project -> project != null
                        && matchesKeyword(project, keyword)
                        && (resolvedTypeId == null || resolvedTypeId.equals(project.getTypeId()))
                        && matchesMyCreated(project, userId, isMyCreated))
                .toList();
    }

    public Long resolveTypeId(Long orgId, String projectTypeName) {
        if (!StringUtils.hasText(projectTypeName)) {
            return null;
        }
        ProjectType projectType = projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, orgId)
                        .eq(ProjectType::getName, projectTypeName.trim())
                        .eq(ProjectType::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
        return projectType != null ? projectType.getId() : null;
    }

    public boolean matchesKeyword(Project project, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return project.getTitle().toLowerCase().contains(keyword.trim().toLowerCase());
    }

    public boolean matchesMyCreated(Project project, Long userId, Boolean isMyCreated) {
        if (isMyCreated == null || !isMyCreated) {
            return true;
        }
        return project.getCreatorId().equals(userId);
    }
}
