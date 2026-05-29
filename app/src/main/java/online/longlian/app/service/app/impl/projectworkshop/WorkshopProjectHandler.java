package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WorkshopProjectHandler {

    private final ProjectTypeMapper projectTypeMapper;

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
}
