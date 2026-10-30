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
 * 产品版本。published_pair_guard 由数据库生成，不进入实体，避免插入生成列。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_release")
@ApiModel(value = "ProductRelease对象", description = "产品版本")
public class ProductRelease implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    @ApiModelProperty("产品版本ID")
    private Long id;

    @TableField("version")
    @ApiModelProperty("产品版本，x.y.z，不含v")
    private String version;

    @TableField("frontend_version")
    @ApiModelProperty("绑定的前端版本，x.y.z，不含v")
    private String frontendVersion;

    @TableField("backend_version")
    @ApiModelProperty("绑定的后端版本，x.y.z，不含v")
    private String backendVersion;

    @TableField("title")
    @ApiModelProperty("标题")
    private String title;

    @TableField("summary")
    @ApiModelProperty("摘要")
    private String summary;

    @TableField("status")
    @ApiModelProperty("状态 0-DRAFT 1-PUBLISHED 2-WITHDRAWN")
    private ProductReleaseStatus status;

    @TableField("published_at")
    @ApiModelProperty("发布时间，发布后不变")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
