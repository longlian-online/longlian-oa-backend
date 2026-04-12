package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import online.longlian.app.common.enumeration.Status;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 组织表
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization")
@ApiModel(value = "Organization对象", description = "组织表")
public class Organization implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    @TableId("id")
    @ApiModelProperty("组织ID")
    private Long id;

    /**
     * 组织名称
     */
    @TableField("name")
    @ApiModelProperty("组织名称")
    private String name;

    /**
     * 组织头像
     */
    @ApiModelProperty("组织头像")
    @TableField("avatar_file_id")
    private Long avatarFileId;

    /**
     * 组织简介
     */
    @ApiModelProperty("组织简介")
    @TableField("description")
    private String description;

    /**
     * 状态 1-启用 0-禁用
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-禁用")
    private Status status;

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
