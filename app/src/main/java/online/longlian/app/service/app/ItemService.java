package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOperationParamsBO;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;

/**
 * 用户端项目（Item）服务接口。
 * <p>
 * 负责任务项目的创建、列表查询和状态管理：
 * <ol>
 *   <li><b>创建项目</b>：根据选定的任务模板实例化完整的任务流：
 *       <ul>
 *         <li>创建 Item（项目实体）</li>
 *         <li>创建 {@link online.longlian.app.pojo.entity.ItemTaskFlow ItemTaskFlow}（任务流快照，记录模板名称和描述）</li>
 *         <li>根据模板节点创建 {@link online.longlian.app.pojo.entity.ItemTaskNode ItemTaskNode}（任务流节点）</li>
 *         <li>为每个节点生成 {@link online.longlian.app.pojo.entity.TaskInstance TaskInstance}（任务实例），
 *             首个节点自动置为 {@code CLAIMED} 并指派给创建者，其余节点置为 {@code PENDING}</li>
 *       </ul>
 *   </li>
 *   <li><b>项目发布</b>：将项目状态从 {@code IN_PROGRESS} 切换为 {@code PUBLISHED}，已发布的项目不可重复发布</li>
 *   <li><b>项目删除</b>：软删除，设置 deletedAt 时间戳</li>
 * </ol>
 */
public interface ItemService {

    /**
     * 分页查询企划下的项目列表。
     *
     * @param params 包含分页参数、企划 ID 及组织 ID 的查询参数
     * @return 分页的项目列表
     */
    PageResultBO<ProjectItemListVO> listProjectItems(ItemListParamsBO params);

    /**
     * 创建项目并实例化任务流。
     * <p>
     * 根据选定的任务模板展开为完整的任务流：创建 Item → ItemTaskFlow → ItemTaskNode → TaskInstance。
     * 首个节点自动接取（{@code CLAIMED}）并指派给创建者，后续节点需等待前序节点完成后按序解锁。
     *
     * @param params 包含企划 ID、项目标题、任务模板 ID 及创建者 ID 的创建参数
     */
    void createProjectItem(ItemCreateParamsBO params);

    /**
     * 软删除项目。
     *
     * @param params 包含项目 ID 和所属企划 ID 的操作参数
     */
    void deleteProjectItem(ItemOperationParamsBO params);

    /**
     * 发布项目，将状态切换为 {@code PUBLISHED}，已发布的项目不可重复发布。
     *
     * @param params 包含项目 ID 和所属企划 ID 的操作参数
     */
    void publishProjectItem(ItemOperationParamsBO params);
}
