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
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 通用文件存储表
 * </p>
 *
 * @author longlian
 * @since 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resource")
@ApiModel(value = "Resource对象", description = "通用文件存储表")
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId("id")
    @ApiModelProperty("文件ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 存储类型 1-本地存储 2-云对象存储
     */
    @TableField("storage_type")
    @ApiModelProperty("存储类型 1-本地存储 2-云对象存储")
    private StorageType storageType;

    /**
     * 存储唯一标识（如OSS的objectKey/本地文件路径）
     */
    @TableField("storage_key")
    @ApiModelProperty("存储唯一标识（如OSS的objectKey/本地文件路径）")
    private String storageKey;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    @ApiModelProperty("原始文件名")
    private String fileName;

    /**
     * 文件扩展名
     */
    @TableField("file_ext")
    @ApiModelProperty("文件扩展名")
    private String fileExt;

    /**
     * 文件大小
     */
    @TableField("file_size")
    @ApiModelProperty("文件大小")
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    @TableField("file_mime")
    @ApiModelProperty("文件MIME类型")
    private String fileMime;

    /**
     * 业务类型（如：avatar/cover/task_submit）
     */
    @TableField("biz_type")
    @ApiModelProperty("业务类型（如：avatar/cover/task_submit）")
    private String bizType;

    /**
     * 业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）
     */
    @TableField("biz_id")
    @ApiModelProperty("业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）")
    private Long bizId;

    /**
     * 状态 0-未上传 1-已上传 3-已废弃
     */
    @TableField("process_status")
    @ApiModelProperty("状态 0-未上传 1-已上传 3-已废弃")
    private FileProcessStatus processStatus;

    /**
     * 上传人ID
     */
    @TableField("creator_id")
    @ApiModelProperty("上传人ID")
    private Long creatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
