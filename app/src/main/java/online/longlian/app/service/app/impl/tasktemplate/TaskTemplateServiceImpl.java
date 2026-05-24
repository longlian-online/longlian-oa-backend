package online.longlian.app.service.app.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;
import online.longlian.app.service.app.TaskTemplateService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateMapper taskTemplateMapper;

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

        return allTemplates.stream()
                .map(t -> {
                    TaskTemplateOptionVO taskTemplateOptionVO = new TaskTemplateOptionVO();
                    taskTemplateOptionVO.setId(t.getId());
                    taskTemplateOptionVO.setName(t.getName());
                    return taskTemplateOptionVO;
                })
                .toList();
    }
}
