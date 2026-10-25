package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOrderEntryResultBO;
import online.longlian.app.pojo.bo.app.ItemOrderQueryParamsBO;
import online.longlian.app.pojo.bo.app.ItemReorderParamsBO;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;

import java.util.List;

/**
 * 用户端项目（Item）服务接口。
 * <p>
 * 负责任务项目的创建、列表查询和删除：
 * <ol>
 *   <li><b>创建项目</b>：根据选定的任务模板实例化完整的任务流：
 *       <ul>
 *         <li>创建 Item（项目实体）</li>
 *         <li>创建 {@link online.longlian.app.pojo.entity.ItemTaskFlow ItemTaskFlow}（任务流快照，记录模板名称和描述）</li>
 *         <li>根据模板节点创建 {@link online.longlian.app.pojo.entity.ItemTaskNode ItemTaskNode}（任务流节点）</li>
 *         <li>为每个节点生成 {@link online.longlian.app.pojo.entity.TaskInstance TaskInstance}（任务实例），
 *             状态均为 {@code PENDING}，不指定接取人</li>
 *       </ul>
 *   </li>
 *   <li><b>审核删除</b>：软删除 Item，并级联软删其下 ItemTaskFlow / ItemTaskNode / TaskInstance</li>
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
     * 所有任务实例创建为待接取（{@code PENDING}），不自动接取。
     *
     * @param params 包含企划 ID、项目标题、任务模板 ID 及创建者 ID 的创建参数
     */
    void createProjectItem(ItemCreateParamsBO params);

    List<ItemOrderEntryResultBO> getProjectItemOrder(ItemOrderQueryParamsBO params);

    void reorderProjectItems(ItemReorderParamsBO params);

    /**
     * 审核通过后软删除项目，不再检查创建者或是否已有提交。
     */
    void deleteProjectItemForReview(Long projectId, Long itemId, Long orgId);

    /**
     * 审核通过后软删除企划下仍存在的全部项目。
     */
    void deleteProjectItemsForReview(Long projectId, Long orgId);

    /**
     * 企划下是否仍有项目。
     */
    boolean hasProjectItems(Long projectId);

    /**
     * 项目是否已有状态为已提交的任务。
     */
    boolean hasSubmittedTask(Long itemId);

    /**
     * 确认项目属于该企划并返回项目名称。
     */
    String requireItemTitle(Long projectId, Long itemId);

}
