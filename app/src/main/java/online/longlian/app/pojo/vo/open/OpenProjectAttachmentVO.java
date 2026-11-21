package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放面企划附件元信息。字段冻结：只增不删；不含读取链接（按次签发接口获取）。
 */
@Data
@Schema(description = "开放接口企划附件")
public class OpenProjectAttachmentVO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "MIME 类型")
    private String mime;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "上传人昵称")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime createdAt;
}
