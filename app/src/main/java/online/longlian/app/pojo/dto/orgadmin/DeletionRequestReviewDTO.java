package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.common.enumeration.DeletionRequestStatus;

@Data
@Schema(description = "审核删除申请")
public class DeletionRequestReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：APPROVED-通过，REJECTED-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private DeletionRequestStatus status;

    @AssertTrue(message = "审核结果只能为 APPROVED 或 REJECTED")
    @Schema(hidden = true)
    public boolean isTerminalStatus() {
        return status == null || status == DeletionRequestStatus.APPROVED
                || status == DeletionRequestStatus.REJECTED;
    }
}
