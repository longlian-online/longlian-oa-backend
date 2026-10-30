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
import online.longlian.common.enumeration.ProductReleaseStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 产品版本
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_release")
@ApiModel(value = "ProductRelease对象", description = "产品版本")
public class ProductRelease implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品版本ID
     */
    @TableId("id")
    @ApiModelProperty("产品版本ID")
    private Long id;

    /**
     * 产品版本，x.y.z，不含v
     */
    @TableField("version")
    @ApiModelProperty("产品版本，x.y.z，不含v")
    private String version;

    /**
     * 绑定的前端版本，x.y.z，不含v
     */
    @TableField("frontend_version")
    @ApiModelProperty("绑定的前端版本，x.y.z，不含v")
    private String frontendVersion;

    /**
     * 绑定的后端版本，x.y.z，不含v
     */
    @TableField("backend_version")
    @ApiModelProperty("绑定的后端版本，x.y.z，不含v")
    private String backendVersion;

    /**
     * 标题，草稿可为空字符串
     */
    @TableField("title")
    @ApiModelProperty("标题，草稿可为空字符串")
    private String title;

    /**
     * 摘要，草稿可为空字符串
     */
    @TableField("summary")
    @ApiModelProperty("摘要，草稿可为空字符串")
    private String summary;

    /**
     * 状态 0-DRAFT(草稿) 1-PUBLISHED(已发布) 2-WITHDRAWN(已撤回)
     */
    @TableField("status")
    @ApiModelProperty("状态 0-DRAFT(草稿) 1-PUBLISHED(已发布) 2-WITHDRAWN(已撤回)")
    private ProductReleaseStatus status;

    /**
     * 发布时间，发布后不变；微秒精度用于区分同一秒内的连续发布
     */
    @TableField("published_at")
    @ApiModelProperty("发布时间，发布后不变；微秒精度用于区分同一秒内的连续发布")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
