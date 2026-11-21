package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "开放接口企划条目")
public class OpenProjectVO {

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

    @Schema(description = "状态：进行中/已完成/已归档")
    private ProjectStatus status;

    @Schema(description = "当前密钥用户是否为企划创建人")
    private boolean creator;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间，作为增量拉取依据")
    private LocalDateTime updatedAt;
}
