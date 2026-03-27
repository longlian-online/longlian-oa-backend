package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.ApplicationStatus;

@Data
@Schema(description = "审核入组申请请求参数")
public class ApplicationReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(
        description = "审核结果：APPROVED-通过，REJECTED-拒绝",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ApplicationStatus applicationStatus;

    @Size(max = 500, message = "审核备注不能超过 500 个字符")
    @Schema(description = "审核备注（拒绝时建议填写原因）")
    private String reviewRemark;
}
