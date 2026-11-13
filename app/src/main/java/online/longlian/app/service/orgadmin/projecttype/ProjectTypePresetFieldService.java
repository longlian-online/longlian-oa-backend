package online.longlian.app.service.orgadmin.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.ProjectTypeCustomFieldMapper;
import online.longlian.app.pojo.entity.ProjectTypeCustomField;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预设字段删除时清理企划类型表单引用，不依赖预设字段读取服务，避免两个域相互依赖。
 */
@Service
@RequiredArgsConstructor
public class ProjectTypePresetFieldService {

    private final ProjectTypeCustomFieldMapper projectTypeCustomFieldMapper;

    @Transactional(rollbackFor = Exception.class)
    public void detachPresetField(Long orgId, Long presetFieldId) {
        projectTypeCustomFieldMapper.delete(new LambdaQueryWrapper<ProjectTypeCustomField>()
                .eq(ProjectTypeCustomField::getOrgId, orgId)
                .eq(ProjectTypeCustomField::getPresetFieldId, presetFieldId));
    }
}
