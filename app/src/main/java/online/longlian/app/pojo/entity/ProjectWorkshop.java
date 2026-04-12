package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * <p>
 * 企划-工坊关联表（用户添加企划）
 * </p>
 *
 * @author longlian
 * @since 2026-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_workshop")
@ApiModel(value = "ProjectWorkshop对象", description = "企划-工坊关联表（用户添加企划）")
public class ProjectWorkshop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 企划ID
     */
    @ApiModelProperty("企划ID")
    @TableField("project_id")
    private Long projectId;

    /**
     * 添加人ID
     */
    @TableField("user_id")
    @ApiModelProperty("添加人ID")
    private Long userId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
