package online.longlian.app.pojo.dto.open;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.PatternConstants;

@Data
@Schema(description = "开放接口创建上传资源请求")
public class OpenFileCreateDTO {

    @NotBlank(message = "原始文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255")
    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @NotBlank(message = "文件扩展名不能为空")
    @Pattern(regexp = PatternConstants.FILE_EXT_PATTERN, message = "不支持的文件类型")
    @Schema(description = "文件扩展名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileExt;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    @Max(value = CommonConstants.FILE_ARCHIVE_MAX_SIZE, message = "文件大小不能超过500MB")
    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @NotBlank(message = "文件MIME类型不能为空")
    @Pattern(regexp = PatternConstants.FILE_MIME_PATTERN, message = "不支持的文件MIME类型")
    @Schema(description = "文件MIME类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileMime;

    /** 大小上限与类型口径同站内；开放面业务类型收窄为提交附件一种。 */
    @NotBlank(message = "业务类型不能为空")
    @Pattern(regexp = "task_submit", message = "开放接口仅支持 task_submit 业务类型")
    @Schema(description = "业务类型，开放面仅支持 task_submit", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;
}
