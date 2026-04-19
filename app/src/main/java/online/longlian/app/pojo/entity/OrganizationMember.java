package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.Status;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 组织成员表
 * </p>
 *
 * @author longlian
 * @since 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_member")
@ApiModel(value = "OrganizationMember对象", description = "组织成员表")
public class OrganizationMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("组织ID")
    private Long orgId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 组织内角色：ORG_ADMIN/ORG_USER
     */
    @TableField("org_role")
    @ApiModelProperty("组织内角色：ORG_ADMIN/ORG_USER")
    private String orgRole;

    /**
     * 入组时间
     */
    @TableField("joined_at")
    @ApiModelProperty("入组时间")
    private LocalDateTime joinedAt;

    /**
     * 上次提交任务时间
     */
    @ApiModelProperty("上次提交任务时间")
    @TableField("last_submitted_at")
    private LocalDateTime lastSubmittedAt;

    /**
     * 任务提交总数
     */
    @ApiModelProperty("任务提交总数")
    @TableField("submit_count")
    private Integer submitCount;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
