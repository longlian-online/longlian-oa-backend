package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "草稿附件")
public class TaskDraftFileVO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MIME类型")
    private String fileMime;

    @Schema(description = "available-可用；unavailable-不可用")
    private String availability;

    @Schema(description = "临时读取链接")
    private String readUrl;

    @Schema(description = "读取链接到期时间(Unix秒)")
    private Long expiresAt;
}
