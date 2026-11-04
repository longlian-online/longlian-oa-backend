package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "企划附件下载链接")
public class ProjectAttachmentDownloadVO {

    @Schema(description = "文件读取地址")
    private String readUrl;

    @Schema(description = "签名链接到期时间，Unix 秒")
    private Long expireAt;
}
