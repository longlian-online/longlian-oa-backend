package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrgFileBatchDownloadParamsBO {
    private Long orgId;
    private List<OrgFileBatchTargetParamsBO> targets;
}
