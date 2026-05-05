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
import online.longlian.generator.enumeration.EmailVerifyBusinessType;
import online.longlian.generator.enumeration.EmailVerifySendStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 邮箱验证码扩展表
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("email_verify_otp")
@ApiModel(value = "EmailVerifyOtp对象", description = "邮箱验证码扩展表")
public class EmailVerifyOtp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱验证码ID
     */
    @TableId("id")
    @ApiModelProperty("邮箱验证码ID")
    private Long id;

    /**
     * 关联验证码ID
     */
    @TableField("otp_id")
    @ApiModelProperty("关联验证码ID")
    private Long otpId;

    /**
     * 接收者邮箱
     */
    @TableField("receiver")
    @ApiModelProperty("接收者邮箱")
    private String receiver;

    /**
     * 业务类型 0-登录 1-注册 2-忘记密码
     */
    @TableField("business_type")
    @ApiModelProperty("业务类型 0-登录 1-注册 2-忘记密码")
    private EmailVerifyBusinessType businessType;

    /**
     * 发送状态 0-待发送 1-发送成功 2-发送失败
     */
    @TableField("send_status")
    @ApiModelProperty("发送状态 0-待发送 1-发送成功 2-发送失败")
    private EmailVerifySendStatus sendStatus;

    /**
     * 发送成功时间
     */
    @TableField("sent_at")
    @ApiModelProperty("发送成功时间")
    private LocalDateTime sentAt;

    /**
     * 发送失败时间
     */
    @TableField("failed_at")
    @ApiModelProperty("发送失败时间")
    private LocalDateTime failedAt;

    /**
     * 发送失败原因
     */
    @TableField("fail_reason")
    @ApiModelProperty("发送失败原因")
    private String failReason;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
