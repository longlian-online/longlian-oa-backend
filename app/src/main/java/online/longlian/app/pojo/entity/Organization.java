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
 * 组织表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("organization")
@Schema(name = "Organization", description = "组织表")
public class Organization implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    @TableId("id")
    @Schema(description = "组织ID")
    private Long id;

    /**
     * 组织名称
     */
    @TableField("name")
    @Schema(description = "组织名称")
    private String name;

    /**
     * 组织头像
     */
    @TableField("avatar_file_id")
    @Schema(description = "组织头像文件ID")
    private Long avatarFileId;

    /**
     * 组织简介
     */
    @TableField("description")
    @Schema(description = "组织简介")
    private String description;

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