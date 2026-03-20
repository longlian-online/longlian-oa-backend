package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import online.longlian.app.common.enumeration.ProjectStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 企划表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("project")
@Schema(name = "Project", description = "企划表")
public class Project implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "企划ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 企划类型ID
     */
    @TableField("type_id")
    @Schema(description = "企划类型ID")
    private Long typeId;

    /**
     * 企划名称
     */
    @TableField("title")
    @Schema(description = "企划名称")
    private String title;

    /**
     * 别名
     */
    @TableField("alias")
    @Schema(description = "别名")
    private String alias;

    /**
     * 企划作者
     */
    @TableField("author")
    @Schema(description = "企划作者")
    private String author;

    /**
     * 封面图
     */
    @TableField("cover_file_id")
    @Schema(description = "封面图文件ID")
    private Long coverFileId;

    /**
     * 简介
     */
    @TableField("description")
    @Schema(description = "简介")
    private String description;

    /**
     * 状态 1-进行中 2-已完成 3-已归档
     */
    @TableField("status")
    @Schema(description = "状态 1-进行中 2-已完成 3-已归档")
    private ProjectStatus status;

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