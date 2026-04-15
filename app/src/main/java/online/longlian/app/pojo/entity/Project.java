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
import online.longlian.app.common.enumeration.ProjectStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 企划表
 * </p>
 *
 * @author longlian
 * @since 2026-04-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project")
@ApiModel(value = "Project对象", description = "企划表")
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 企划类型ID
     */
    @TableField("type_id")
    @ApiModelProperty("企划类型ID")
    private Long typeId;

    /**
     * 企划名称
     */
    @TableField("title")
    @ApiModelProperty("企划名称")
    private String title;

    /**
     * 别名
     */
    @TableField("alias")
    @ApiModelProperty("别名")
    private String alias;

    /**
     * 扩展信息(JSON字符串)
     */
    @TableField("metadata")
    @ApiModelProperty("扩展信息(JSON字符串)")
    private String metadata;

    /**
     * 封面图
     */
    @ApiModelProperty("封面图")
    @TableField("cover_file_id")
    private Long coverFileId;

    /**
     * 简介
     */
    @ApiModelProperty("简介")
    @TableField("description")
    private String description;

    /**
     * 状态 1-进行中 2-已完成 3-已归档
     */
    @TableField("status")
    @ApiModelProperty("状态 1-进行中 2-已完成 3-已归档")
    private ProjectStatus status;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    @ApiModelProperty("创建人ID")
    private Long creatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
