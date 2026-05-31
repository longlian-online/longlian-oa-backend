package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;

import java.util.List;

/**
 * 用户端任务模板服务接口。
 * <p>
 * 提供创建项目时选择任务模板的下拉选项：
 * 合并展示当前组织下所有启用的组织级模板和当前用户创建的个人级模板，
 * 按创建时间倒序排列。
 */
public interface TaskTemplateService {

    /**
     * 获取可选任务模板列表。
     * <p>
     * 返回当前组织启用的组织级模板 + 当前用户创建的个人级模板，
     * 作为创建项目时的模板下拉选项。
     *
     * @param params 包含用户 ID 和组织 ID 的查询参数
     * @return 模板 ID 和名称的选项列表
     */
    List<TaskTemplateOptionVO> listOptions(TaskTemplateOptionsParamsBO params);
}
