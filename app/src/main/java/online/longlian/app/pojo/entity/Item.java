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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 项目表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@TableName("item")
@AllArgsConstructor
@ApiModel(value = "Item对象", description = "项目表")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 所属企划ID
     */
    @TableField("project_id")
    @ApiModelProperty("所属企划ID")
    private Long projectId;

    /**
     * 项目名称
     */
    @TableField("title")
    @ApiModelProperty("项目名称")
    private String title;

    /**
     * 企划内项目展示顺序
     */
    @TableField("sort")
    @ApiModelProperty("企划内项目展示顺序")
    private Long sort;

    /**
     * 创建时关联的任务模板ID（仅记录溯源）
     */
    @TableField("task_template_id")
    @ApiModelProperty("创建时关联的任务模板ID（仅记录溯源）")
    private Long taskTemplateId;

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
