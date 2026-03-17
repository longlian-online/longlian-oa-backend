package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import online.longlian.app.common.enumeration.FileProcessStatus;
import online.longlian.app.common.enumeration.StorageType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 通用文件存储表
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Getter
@Setter
@ToString
@TableName("file_storage")
@Schema(name = "FileStorage", description = "通用文件存储表")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId("id")
    @Schema(description = "文件ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @Schema(description = "所属组织ID")
    private Long orgId;

    /**
     * 存储类型 1-本地存储 2-阿里云OSS 3-腾讯云COS
     */
    @TableField("storage_type")
    @Schema(description = "存储类型 1-本地存储 2-阿里云OSS 3-腾讯云COS")
    private StorageType storageType;

    /**
     * 存储唯一标识（如OSS的objectKey/本地文件路径）
     */
    @TableField("storage_key")
    @Schema(description = "存储唯一标识（如OSS的objectKey/本地文件路径）")
    private String storageKey;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * 文件扩展名
     */
    @TableField("file_ext")
    @Schema(description = "文件扩展名")
    private String fileExt;

    /**
     * 文件大小
     */
    @TableField("file_size")
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    @TableField("file_mime")
    @Schema(description = "文件MIME类型")
    private String fileMime;

    /**
     * 业务类型（如：avatar/cover/task_submit）
     */
    @TableField("biz_type")
    @Schema(description = "业务类型（如：avatar/cover/task_submit）")
    private String bizType;

    /**
     * 业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）
     */
    @TableField("biz_id")
    @Schema(description = "业务ID（关联的用户ID/组织ID/企划ID/任务提交ID）")
    private Long bizId;

    /**
     * 文件处理状态 0-未处理 1-处理中 2-已压缩 3-处理失败
     */
    @TableField("process_status")
    @Schema(description = "文件处理状态 0-未处理 1-处理中 2-已压缩 3-处理失败")
    private FileProcessStatus processStatus;

    /**
     * 是否被引用 1-是 0-否（清理无用文件）
     */
    @TableField("is_referenced")
    @Schema(description = "是否被引用 1-是 0-否（清理无用文件）")
    private Integer isReferenced;

    /**
     * 上传人ID
     */
    @TableField("creator_id")
    @Schema(description = "上传人ID")
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