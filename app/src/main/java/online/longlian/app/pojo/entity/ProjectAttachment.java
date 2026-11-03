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
 * 企划附件关联表（文件元信息复用 resource）
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_attachment")
@ApiModel(value = "ProjectAttachment对象", description = "企划附件关联表（文件元信息复用 resource）")
public class ProjectAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 企划附件ID
     */
    @TableId("id")
    @ApiModelProperty("企划附件ID")
    private Long id;

    /**
     * 企划ID
     */
    @ApiModelProperty("企划ID")
    @TableField("project_id")
    private Long projectId;

    /**
     * 附件文件ID（resource.id）
     */
    @TableField("file_id")
    @ApiModelProperty("附件文件ID（resource.id）")
    private Long fileId;

    /**
     * 上传人ID
     */
    @ApiModelProperty("上传人ID")
    @TableField("uploader_id")
    private Long uploaderId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
