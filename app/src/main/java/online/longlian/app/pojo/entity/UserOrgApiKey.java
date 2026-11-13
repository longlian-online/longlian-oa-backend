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
 * 用户组织MCP访问凭据
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_org_api_key")
@ApiModel(value = "UserOrgApiKey对象", description = "用户组织MCP访问凭据")
public class UserOrgApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("id")
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    @ApiModelProperty("所属用户ID")
    private Long userId;

    /**
     * 绑定组织ID
     */
    @TableField("org_id")
    @ApiModelProperty("绑定组织ID")
    private Long orgId;

    /**
     * 完整Key的SHA-256摘要，撤销后清空
     */
    @TableField("key_hash")
    @ApiModelProperty("完整Key的SHA-256摘要，撤销后清空")
    private String keyHash;

    /**
     * 用于脱敏展示的Key前缀
     */
    @TableField("key_prefix")
    @ApiModelProperty("用于脱敏展示的Key前缀")
    private String keyPrefix;

    /**
     * 最近一次Key生成时间
     */
    @TableField("generated_at")
    @ApiModelProperty("最近一次Key生成时间")
    private LocalDateTime generatedAt;

    /**
     * 撤销时间
     */
    @ApiModelProperty("撤销时间")
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
