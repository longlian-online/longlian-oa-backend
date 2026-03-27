package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工坊中的企划信息")
public class WorkshopProjectInfoVO {
    @Schema(description = "企划名称")
    private String title;
    @Schema(description = "封面图片URL")
    private String coverUrl;
    @Schema(description = "创建人头像URL")
    private String creatorAvatarUrl;
    @Schema(description = "最近一次提交的用户名")
    private String lastSubmitterUsername;
    @Schema(description = "最近一次提交的时间")
    private LocalDateTime lastSubmitterAt;
}
