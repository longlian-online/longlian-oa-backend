package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminCreateParamsBO;
import online.longlian.app.pojo.bo.admin.AdminListParamsBO;
import online.longlian.app.pojo.bo.admin.AdminListResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

/**
 * 系统管理端管理员帐号管理服务接口。
 * <p>
 * 负责管理员帐号的创建、删除和列表查询：
 * <ol>
 *   <li><b>创建</b>：仅 root 管理员可创建新管理员，新管理员默认角色为 {@code normal}（无 root 权限）</li>
 *   <li><b>删除</b>：仅 root 管理员可删除，且不允许删除 root 帐号或删除自己；
 *       删除时通过 {@link online.longlian.app.service.TokenBlacklistService} 全局拉黑该管理员所有 Token</li>
 *   <li><b>列表</b>：分页查询所有未删除的管理员列表</li>
 * </ol>
 */
public interface AdminManagementService {

    /**
     * 创建管理员，仅 root 管理员可操作。
     *
     * @param params     包含用户名和密码的创建参数
     * @param operatorId 操作者 ID（须为 root）
     * @return 新创建管理员的 ID
     */
    Long create(AdminCreateParamsBO params, Long operatorId);

    /**
     * 删除管理员（逻辑删除）。
     * <p>
     * 仅 root 管理员可操作，不能删除 root 帐号或删除自己。
     * 删除时会将该管理员所有 Token 加入黑名单。
     *
     * @param id         待删除的管理员 ID
     * @param operatorId 操作者 ID（须为 root）
     */
    void delete(Long id, Long operatorId);

    /**
     * 分页查询管理员列表（不含已删除的）。
     *
     * @param params 包含分页参数的查询参数
     * @return 分页的管理员列表
     */
    PageResultBO<AdminListResultBO> list(AdminListParamsBO params);
}
