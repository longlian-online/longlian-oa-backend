package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 任务提交附件的展示信息。读取链接只在点击预览或下载时另行签发。 */
@Data
@Schema(description = "任务提交附件；不包含读取链接")
public class TaskAttachmentVO {

    @Schema(description = "附件文件ID")
    private String id;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "文件大小展示文本")
    private String sizeText;

    @Schema(description = "媒体类型：image/document/archive/other")
    private String mediaType;

    @Schema(description = "可读状态：available-可读，unavailable-不可用")
    private String availability;
}
