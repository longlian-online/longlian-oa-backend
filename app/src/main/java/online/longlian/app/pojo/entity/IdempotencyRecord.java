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
import online.longlian.common.enumeration.IdempotencySource;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 写请求幂等记录表，按来源区分调用方
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("idempotency_record")
@ApiModel(value = "IdempotencyRecord对象", description = "写请求幂等记录表，按来源区分调用方")
public class IdempotencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;

    /**
     * 来源 1-OPEN_API
     */
    @TableField("source")
    @ApiModelProperty("来源 1-OPEN_API")
    private IdempotencySource source;

    /**
     * 来源内调用方标识，OPEN_API 时为 open_api_key.id
     */
    @TableField("caller_id")
    @ApiModelProperty("来源内调用方标识，OPEN_API 时为 open_api_key.id")
    private Long callerId;

    /**
     * 调用方生成的幂等键
     */
    @ApiModelProperty("调用方生成的幂等键")
    @TableField("idempotency_key")
    private String idempotencyKey;

    /**
     * HTTP 方法
     */
    @TableField("method")
    @ApiModelProperty("HTTP 方法")
    private String method;

    /**
     * 接口路径，不含查询串
     */
    @TableField("path")
    @ApiModelProperty("接口路径，不含查询串")
    private String path;

    /**
     * 请求体原文的 SHA-256
     */
    @TableField("body_hash")
    @ApiModelProperty("请求体原文的 SHA-256")
    private String bodyHash;

    /**
     * 首次成功响应的完整 Result 信封；超 64KB 不存正文
     */
    @TableField("snapshot")
    @ApiModelProperty("首次成功响应的完整 Result 信封；超 64KB 不存正文")
    private String snapshot;

    /**
     * 1-响应超限未存正文，重复请求只返回命中标记
     */
    @TableField("snapshot_truncated")
    @ApiModelProperty("1-响应超限未存正文，重复请求只返回命中标记")
    private Byte snapshotTruncated;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
