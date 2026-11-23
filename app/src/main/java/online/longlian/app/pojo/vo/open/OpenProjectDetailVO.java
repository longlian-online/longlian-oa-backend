package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

/**
 * 开放面企划详情。字段冻结：只增不删；不含管理视角字段与统计。
 */
@Data
@Schema(description = "开放接口企划详情")
public class OpenProjectDetailVO {

    @Schema(description = "企划ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "别名")
    private String alias;

    @Schema(description = "封面读取链接，按次签发，短期有效")
    private String coverUrl;

    @Schema(description = "缩略图读取链接，按次签发，短期有效")
    private String thumbnailUrl;

    @Schema(description = "企划类型ID")
    private Long typeId;

    @Schema(description = "状态：进行中/已完成/已归档")
    private ProjectStatus status;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "当前密钥用户是否为企划创建人")
    private boolean creator;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
