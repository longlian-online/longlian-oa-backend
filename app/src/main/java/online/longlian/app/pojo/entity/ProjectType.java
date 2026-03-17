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
 * 企划类型表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("project_type")
@Schema(name = "ProjectType", description = "企划类型表")
public class ProjectType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 企划类型ID
     */
    @TableId("id")
    @Schema(description = "企划类型ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 类型名称（如：漫画/小说/美术/视频）
     */
    @TableField("name")
    @Schema(description = "类型名称（如：漫画/小说/美术/视频）")
    private String name;

    /**
     * 排序
     */
    @TableField("sort")
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @Schema(description = "状态 1-启用 0-禁用")
    private Byte status;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    @Schema(description = "创建人ID")
    private Long creatorId;

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