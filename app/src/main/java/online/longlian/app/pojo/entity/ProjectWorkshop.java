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
 * 企划-工坊关联表（用户添加企划）
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("project_workshop")
@Schema(name = "ProjectWorkshop", description = "企划-工坊关联表（用户添加企划）")
public class ProjectWorkshop implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 企划ID
     */
    @TableField("project_id")
    @Schema(description = "企划ID")
    private Long projectId;

    /**
     * 添加人ID
     */
    @TableField("user_id")
    @Schema(description = "添加人ID")
    private Long userId;

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