package online.longlian.app.pojo.bo.orgadmin.file;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrgFileExpansionParamsBO {
    private Long orgId;
    private List<OrgFileBatchTargetParamsBO> targets;
}
