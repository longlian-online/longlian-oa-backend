package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.OrgFileBatchTargetKind;

@Data
@Builder
public class OrgFileBatchTargetParamsBO {
    private OrgFileBatchTargetKind kind;
    private Long taskInstanceId;
    private Long fileId;
    private String fieldKey;
    private Long itemId;
    private Long projectId;
    private Long typeId;
}
