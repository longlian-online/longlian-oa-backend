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
import online.longlian.common.enumeration.ProductReleaseChangeKind;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_release_change")
@ApiModel(value = "ProductReleaseChange对象", description = "产品版本变更条目")
public class ProductReleaseChange implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    @ApiModelProperty("变更条目ID")
    private Long id;

    @TableField("release_id")
    @ApiModelProperty("产品版本ID")
    private Long releaseId;

    @TableField("kind")
    @ApiModelProperty("分组 1-ADDED 2-CHANGED 3-FIXED")
    private ProductReleaseChangeKind kind;

    @TableField("sort")
    @ApiModelProperty("组内顺序，从0开始")
    private Integer sort;

    @TableField("content")
    @ApiModelProperty("条目正文")
    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
