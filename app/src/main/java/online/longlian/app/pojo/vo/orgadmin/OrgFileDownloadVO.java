package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "组织文件页的提交文件读取链接")
public class OrgFileDownloadVO {
    @Schema(description = "文件读取地址")
    private String readUrl;

    @Schema(description = "签名链接到期时间，Unix 秒")
    private Long expireAt;
}
