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
 * 用户产品版本已读位置
 * </p>
 *
 * @author longlian
 * @since 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_product_release_state")
@ApiModel(value = "UserProductReleaseState对象", description = "用户产品版本已读位置")
public class UserProductReleaseState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId("user_id")
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 已读到的发布时间，只向前移动；与发布时间同为微秒精度
     */
    @TableField("last_seen_published_at")
    @ApiModelProperty("已读到的发布时间，只向前移动；与发布时间同为微秒精度")
    private LocalDateTime lastSeenPublishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
