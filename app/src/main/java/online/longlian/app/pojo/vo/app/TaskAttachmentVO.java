package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 任务提交附件的展示信息。读取链接只在点击预览或下载时另行签发。 */
@Data
@Schema(description = "任务提交附件；不包含读取链接")
public class TaskAttachmentVO {
    private String id;
    private String name;
    private String sizeText;
    private String mediaType;
    private String availability;
}
