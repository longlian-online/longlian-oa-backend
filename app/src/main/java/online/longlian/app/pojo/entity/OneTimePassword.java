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
import online.longlian.common.enumeration.OPTStatus;
import online.longlian.common.enumeration.OTPType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 一次性密码（otp）表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("one_time_password")
@ApiModel(value = "OneTimePassword对象", description = "一次性密码（otp）表")
public class OneTimePassword implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 验证码
     */
    @TableField("code")
    @ApiModelProperty("验证码")
    private String code;

    /**
     * 过期时间
     */
    @ApiModelProperty("过期时间")
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /**
     * 使用时间
     */
    @TableField("used_at")
    @ApiModelProperty("使用时间")
    private LocalDateTime usedAt;

    /**
     * 业务类型 1.邀请创建组织 2.邀请加入组织
     */
    @TableField("biz_type")
    @ApiModelProperty("业务类型 1.邀请创建组织 2.邀请加入组织")
    private OTPType bizType;

    /**
     * 状态 0-待使用 1-已使用
     */
    @TableField("status")
    @ApiModelProperty("状态 0-待使用 1-已使用")
    private OPTStatus status;

    /**
     * 创建者 ID
     */
    @TableField("creator_id")
    @ApiModelProperty("创建者 ID")
    private Long creatorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
