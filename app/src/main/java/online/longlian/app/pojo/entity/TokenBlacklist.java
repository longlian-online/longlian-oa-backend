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
import online.longlian.common.enumeration.TokenType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Token黑名单表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("token_blacklist")
@ApiModel(value = "TokenBlacklist对象", description = "Token黑名单表")
public class TokenBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("id")
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * Token字符串
     */
    @TableField("token")
    @ApiModelProperty("Token字符串")
    private String token;

    /**
     * Token类型 1-用户端 2-管理端
     */
    @TableField("token_type")
    @ApiModelProperty("Token类型 1-用户端 2-管理端")
    private TokenType tokenType;

    /**
     * 用户/管理员ID
     */
    @TableField("user_id")
    @ApiModelProperty("用户/管理员ID")
    private Long userId;

    /**
     * 加入黑名单原因
     */
    @TableField("reason")
    @ApiModelProperty("加入黑名单原因")
    private String reason;

    /**
     * Token过期时间
     */
    @TableField("expired_at")
    @ApiModelProperty("Token过期时间")
    private LocalDateTime expiredAt;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
