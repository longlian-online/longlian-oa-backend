package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.generator.enumeration.ApplicationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminReviewApplicationParamsBO {
    private Long applicationId;
    private Long orgId;
    private Long reviewerId;
    private ApplicationStatus applicationStatus;
    private String reviewRemark;
}
