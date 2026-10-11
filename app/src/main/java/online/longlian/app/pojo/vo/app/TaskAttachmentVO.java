package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Authorized task file display data; unavailable files never contain signed access credentials. */
@Data
@Schema(description = "任务提交附件；不可用附件不包含读取链接和有效期")
public class TaskAttachmentVO {
    private String id;
    private String name;
    private String sizeText;
    private String mediaType;
    private String availability;
    private String readUrl;

    @Schema(description = "签名链接到期时间，Unix 秒")
    private Long expiresAt;
}
