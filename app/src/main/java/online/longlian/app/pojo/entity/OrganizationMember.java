package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 组织成员表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("organization_member")
@Schema(name = "OrganizationMember", description = "组织成员表")
public class OrganizationMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "组织成员ID")
    private Long id;

    /**
     * 组织ID
     */
    @TableField("org_id")
    @Schema(description = "组织ID")
    private Long orgId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 组织内角色：ORG_ADMIN/ORG_USER
     */
    @TableField("org_role")
    @Schema(description = "组织内角色：ORG_ADMIN/ORG_USER")
    private String orgRole;

    /**
     * 入组时间
     */
    @TableField("joined_at")
    @Schema(description = "入组时间")
    private LocalDateTime joinedAt;

    /**
     * 上次提交任务时间
     */
    @TableField("last_submitted_at")
    @Schema(description = "上次提交任务时间")
    private LocalDateTime lastSubmittedAt;

    /**
     * 任务提交总数
     */
    @TableField("submit_count")
    @Schema(description = "任务提交总数")
    private Integer submitCount;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
    private Byte status;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}