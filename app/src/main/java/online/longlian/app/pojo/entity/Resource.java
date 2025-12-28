package online.longlian.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import online.longlian.app.common.enumeration.ResourceStatus;
import online.longlian.app.common.enumeration.ResourceStorageType;
import online.longlian.app.common.enumeration.ResourceType;

import java.io.Serializable;

/**
 * <p>
 * 资源存储表
 * </p>
 *
 * @author longlian
 * @since 2025-12-28
 */
@Getter
@Setter
@ToString
@Builder
@TableName("resource")
@ApiModel(value = "Resource对象", description = "资源存储表")
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资源唯一ID（CUID）
     */
    @TableId("id")
    @ApiModelProperty("资源唯一ID（CUID）")
    private Long id;

    /**
     * 存储类型：LOCAL/COS
     */
    @TableField("storage_type")
    @ApiModelProperty("存储类型：LOCAL/COS")
    private ResourceStorageType storageType;

    /**
     * 拓展信息（JSON格式）
     */
    @TableField("extend")
    @ApiModelProperty("拓展信息（JSON格式）")
    private String extend;

    /**
     * 资源路径/存储键
     */
    @TableField("key")
    @ApiModelProperty("资源路径/存储键")
    private String key;

    /**
     * 文件大小（字节）
     */
    @TableField("size")
    @ApiModelProperty("文件大小（字节）")
    private Integer size;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    @ApiModelProperty("所属用户ID")
    private String userId;

    /**
     * 所属分组ID
     */
    @TableField("group_id")
    @ApiModelProperty("所属分组ID")
    private String groupId;

    /**
     * 资源状态
     */
    @TableField("status")
    @ApiModelProperty("资源状态")
    private ResourceStatus status;

    /**
     * 资源类型
     */
    @TableField("type")
    @ApiModelProperty("资源类型")
    private ResourceType type;

}
