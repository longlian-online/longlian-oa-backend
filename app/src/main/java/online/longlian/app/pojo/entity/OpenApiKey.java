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
import online.longlian.common.enumeration.OpenApiKeyStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * OpenAPI 密钥表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("open_api_key")
@ApiModel(value = "OpenApiKey对象", description = "OpenAPI 密钥表")
public class OpenApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 密钥ID
     */
    @TableId("id")
    @ApiModelProperty("密钥ID")
    private Long id;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("所属组织ID")
    private Long orgId;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    @ApiModelProperty("所属用户ID")
    private Long userId;

    /**
     * 密钥名称
     */
    @TableField("name")
    @ApiModelProperty("密钥名称")
    private String name;

    /**
     * 密钥明文的 SHA-256 十六进制，明文不落库
     */
    @TableField("key_hash")
    @ApiModelProperty("密钥明文的 SHA-256 十六进制，明文不落库")
    private String keyHash;

    /**
     * 状态 1-启用 0-吊销
     */
    @TableField("status")
    @ApiModelProperty("状态 1-启用 0-吊销")
    private OpenApiKeyStatus status;

    /**
     * 最近一次成功认证时间，异步更新
     */
    @TableField("last_used_at")
    @ApiModelProperty("最近一次成功认证时间，异步更新")
    private LocalDateTime lastUsedAt;

    /**
     * 吊销时间
     */
    @ApiModelProperty("吊销时间")
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
